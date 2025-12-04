package com.datadistributor.domain.service;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventRetryUseCase;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventPort;
import com.datadistributor.domain.outport.SignalEventSenderPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Retries delivery for failed signal events of a given date. Pulls failed audit entries for that
 * day, resolves the corresponding signal events, and re-sends them one by one using the configured
 * sender (blocking or reactive under the hood). Missing events are counted as failures.
 *
 * <p>Example: if uabsEventId 10 failed twice on 2024-12-03, this service will fetch that id,
 * reload the event, send it once, and mark success/failure in the result; if the event row is gone,
 * it counts as a failure.</p>
 */
@Slf4j
public class SignalEventRetryService implements SignalEventRetryUseCase {

  private final SignalAuditQueryPort signalAuditQueryPort;
  private final SignalEventPort signalEventRepository;
  private final SignalEventSenderPort senderPort;

  public SignalEventRetryService(SignalAuditQueryPort signalAuditQueryPort,
                                 SignalEventPort signalEventRepository,
                                 SignalEventSenderPort senderPort) {
    this.signalAuditQueryPort = signalAuditQueryPort;
    this.signalEventRepository = signalEventRepository;
    this.senderPort = senderPort;
  }

  @Override
  public JobResult retryFailedEvents(String jobId, LocalDate date) {
    if (date == null) {
      return new JobResult(0, 0, 0, "Date is required");
    }

    List<Long> failedIds = signalAuditQueryPort.findFailedEventIdsForDate(date);
    if (failedIds.isEmpty()) {
      return new JobResult(0, 0, 0, "No failed events to retry for " + date);
    }

    List<Long> distinctIds = failedIds.stream().filter(Objects::nonNull).distinct().toList();
    List<SignalEvent> foundEvents = signalEventRepository.findByUabsEventIdIn(distinctIds);
    int missing = distinctIds.size() - foundEvents.size();

    if (foundEvents.isEmpty()) {
      return new JobResult(0, missing, distinctIds.size(), "Failed audit entries found but no matching signal events for " + date);
    }

    Map<Long, SignalEvent> byId = foundEvents.stream()
        .filter(event -> event.getUabsEventId() != null)
        .collect(Collectors.toMap(SignalEvent::getUabsEventId, e -> e, (first, second) -> first));

    List<SignalEvent> toSend = distinctIds.stream()
        .map(byId::get)
        .filter(Objects::nonNull)
        .sorted(Comparator
            .comparing(SignalEvent::getEventRecordDateTime, Comparator.nullsLast(LocalDateTime::compareTo))
            .thenComparing(SignalEvent::getUabsEventId, Comparator.nullsLast(Long::compareTo)))
        .toList();

    int success = 0;
    int failure = missing; // missing counted as failures

    for (SignalEvent event : toSend) {
      boolean sent = senderPort.send(event);
      if (sent) {
        success++;
      } else {
        failure++;
      }
    }

    JobResult result = new JobResult(success, failure, distinctIds.size(),
        "Retry complete for " + date + (missing > 0 ? (" | missing=" + missing) : ""));

    log.info("🔁 Retry finished for {} success={} failure={} (missing={})",
        date, result.getSuccessCount(), result.getFailureCount(), missing);
    return result;
  }
}
