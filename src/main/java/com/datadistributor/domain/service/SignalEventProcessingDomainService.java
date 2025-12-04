package com.datadistributor.domain.service;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventPort;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.report.DeliveryReport;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * Coordinates daily signal-event delivery to CEH: validates prerequisites, selects events, batches
 * them to the outbound client, and emits a delivery report. The selector can return zero items,
 * in which case the legacy paging flow is used for backward compatibility.
 *
 * <p>Prerequisite rule: a signal event for a date is blocked if its prior event (same signal) is
 * not marked PASS in audit for the CEH consumer. Example: if a 2024-12-02 event failed delivery,
 * the 2024-12-03 event will not be sent until the prior one succeeds.</p>
 */
@Slf4j
public class SignalEventProcessingDomainService implements SignalEventProcessingUseCase {

  private final SignalEventPort signalEventRepository;
  private final SignalEventBatchPort signalEventBatchPort;
  private final SignalAuditQueryPort signalAuditQueryPort;
  private final SignalDispatchSelectorUseCase signalDispatchSelector;
  private final int batchSize;
  private final JobProgressTracker jobProgressTracker;
  private final DeliveryReportPublisher deliveryReportPublisher;

  public SignalEventProcessingDomainService(SignalEventPort signalEventRepository,
                                            SignalEventBatchPort signalEventBatchPort,
                                            SignalAuditQueryPort signalAuditQueryPort,
                                            SignalDispatchSelectorUseCase signalDispatchSelector,
                                            int batchSize,
                                            JobProgressTracker jobProgressTracker,
                                            DeliveryReportPublisher deliveryReportPublisher) {
    this.signalEventRepository = signalEventRepository;
    this.signalEventBatchPort = signalEventBatchPort;
    this.signalAuditQueryPort = signalAuditQueryPort;
    this.signalDispatchSelector = signalDispatchSelector;
    this.batchSize = Math.max(1, batchSize);
    this.jobProgressTracker = jobProgressTracker;
    this.deliveryReportPublisher = deliveryReportPublisher;
  }

  /**
   * Processes all events for the given date. Steps:
   * <ol>
   *   <li>Validate prior events (blocks if yesterday's event for the same signal is not PASS).</li>
   *   <li>Select events via the dispatch selector; if none, fall back to legacy paging.</li>
   *   <li>Submit batches to the outbound sender and wait for completion.</li>
   *   <li>Publish a delivery report (success/failure totals).</li>
   * </ol>
   *
   * @param jobId optional tracking id
   * @param date processing date (required)
   * @return summary result (success, failure, total, message)
   */
  @Override
  public JobResult processEventsForDate(String jobId, LocalDate date) {
    if (date == null) {
      return new JobResult(0, 0, 0, "Date is required");
    }
    Optional<String> validationError = validatePriorEvents(date);
    if (validationError.isPresent()) {
      log.error("LOG_003: Batch aborted as previous events are pending for date {} | reason={}", date, validationError.get());
      return new JobResult(0, 0, 0, validationError.get());
    }
    List<SignalEvent> toSend = signalDispatchSelector.selectEventsToSend(date);
    long totalCount = toSend.size();
    boolean usedSelector = true;
    if (totalCount == 0) {
      // fallback to legacy CEH page flow if selector produces no items
      totalCount = signalEventRepository.countSignalEventsForCEH(date);
      if (totalCount == 0) {
        return new JobResult(0, 0, 0, "No signal events found for date " + date);
      }
      usedSelector = false;
    }

    int totalBatches = (int) Math.ceil((double) totalCount / batchSize);
    log.info("🚀 Starting processing for {} events on {} (~{} batches)", totalCount, date, totalBatches);
    List<TrackedBatch> trackedBatches = new ArrayList<>();
    AtomicInteger batchCounter = new AtomicInteger();

    JobProgressTracker.JobProgress progress =
        jobProgressTracker.start(Optional.ofNullable(jobId).orElse(null), totalBatches);

    if (usedSelector) {
      List<List<SignalEvent>> chunks = chunk(toSend, batchSize);
      for (List<SignalEvent> chunk : chunks) {
        trackedBatches.add(submitBatch(new ArrayList<>(chunk), batchCounter.incrementAndGet()));
      }
    } else {
      int page = 0;
      while (true) {
        List<SignalEvent> chunk = signalEventRepository.getSignalEventsForCEH(date, page, batchSize);
        if (chunk.isEmpty()) break;
        trackedBatches.add(submitBatch(new ArrayList<>(chunk), batchCounter.incrementAndGet()));
        page++;
      }
    }

    attachProgressLogging(progress, trackedBatches);

    JobResult result = awaitJobCompletion(trackedBatches, "Processing complete for " + date, totalCount);
    log.info("✅ Processing finished for {}. success={} failure={}",
        date, result.getSuccessCount(), result.getFailureCount());
    publishReport(date, result);
    return result;
  }

  private TrackedBatch submitBatch(List<SignalEvent> batch, int batchNumber) {
    String ids = batch.stream()
        .map(SignalEvent::getUabsEventId)
        .map(String::valueOf)
        .collect(Collectors.joining(","));
    log.info("Submitting batch #{} (size {}) on thread {} | uabsEventIds=[{}]",
        batchNumber, batch.size(), Thread.currentThread().getName(), ids);
    CompletableFuture<BatchResult> future = signalEventBatchPort.submitBatch(batch);
    return new TrackedBatch(future, batchNumber, batch.size());
  }

  private void attachProgressLogging(JobProgressTracker.JobProgress progress,
                                     List<TrackedBatch> trackedBatches) {
    trackedBatches.forEach(batch ->
        batch.future().thenAccept(result ->
            jobProgressTracker.onBatchCompletion(progress, batch.number(), batch.size(), result)));
  }

  private JobResult awaitJobCompletion(List<TrackedBatch> trackedBatches, String message, long totalCount) {
    if (trackedBatches.isEmpty()) {
      return new JobResult(0, 0, totalCount, message);
    }

    List<CompletableFuture<BatchResult>> futures = trackedBatches.stream()
        .map(TrackedBatch::future)
        .collect(Collectors.toList());

    CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    all.join();

    List<BatchResult> results = futures.stream()
        .map(CompletableFuture::join)
        .collect(Collectors.toList());

    int success = results.stream()
        .mapToInt(BatchResult::successCount)
        .sum();

    int failure = results.stream()
        .mapToInt(BatchResult::failureCount)
        .sum();

    return new JobResult(success, failure, totalCount, message);
  }

  private record TrackedBatch(CompletableFuture<BatchResult> future, int number, int size) {
  }

  private List<List<SignalEvent>> chunk(List<SignalEvent> events, int size) {
    List<List<SignalEvent>> chunks = new ArrayList<>();
    for (int i = 0; i < events.size(); i += size) {
      chunks.add(events.subList(i, Math.min(events.size(), i + size)));
    }
    return chunks;
  }

  /**
   * Ensures that for any event on {@code date}, its immediately preceding event for the same
   * signal has a PASS audit. Example: if a signal has events on Dec 2 and Dec 3, the Dec 3 event is
   * blocked when the Dec 2 audit is missing or not PASS.
   */
  private Optional<String> validatePriorEvents(LocalDate date) {
    List<SignalEvent> eventsForDate = signalEventRepository.getAllSignalEventsOfThisDate(date);
    if (eventsForDate.isEmpty()) {
      return Optional.empty();
    }

    List<SignalEvent> missingPrereq = new ArrayList<>();

    eventsForDate.stream()
        .sorted(Comparator.comparing(SignalEvent::getSignalId)
            .thenComparing(SignalEvent::getEventRecordDateTime, Comparator.nullsLast(LocalDateTime::compareTo))
            .thenComparing(SignalEvent::getUabsEventId, Comparator.nullsLast(Long::compareTo)))
        .forEach(event -> {
          Optional<SignalEvent> prev = signalEventRepository.getPreviousEvent(
              event.getSignalId(), event.getEventRecordDateTime());
          if (prev.isPresent()) {
            boolean ok = signalAuditQueryPort.isEventSuccessful(prev.get().getUabsEventId(), 1L);
            if (!ok) {
              missingPrereq.add(event);
            }
          }
        });

    if (missingPrereq.isEmpty()) {
      return Optional.empty();
    }

    String ids = missingPrereq.stream()
        .map(SignalEvent::getUabsEventId)
        .map(String::valueOf)
        .collect(Collectors.joining(","));
    String message = "Prerequisite check failed for date " + date
        + ". Prior event not successful for uabsEventIds=[" + ids + "]";
    log.warn(message);
    return Optional.of(message);
  }

  /**
   * Publishes a simple text report to the configured publisher. Failures to publish are logged but
   * do not fail the job.
   */
  private void publishReport(LocalDate date, JobResult result) {
    if (deliveryReportPublisher == null) {
      return;
    }
    try {
      String content = buildReportContent(date, result);
      DeliveryReport report = DeliveryReport.builder()
          .date(date)
          .totalEvents(result.getTotalCount())
          .successEvents(result.getSuccessCount())
          .failedEvents(result.getFailureCount())
          .content(content)
          .build();
      deliveryReportPublisher.publish(report);
    } catch (Exception ex) {
      log.error("Failed to publish delivery report for {}: {}", date, ex.getMessage(), ex);
    }
  }

  /** Builds the CEH delivery report body. */
  private String buildReportContent(LocalDate date, JobResult result) {
    return """
        UABS DELIVERY TO CEH REPORT
        Total number of events for Date %s = %d
        Total number of events sent to CEH with PASS status- %d
        Total number of events not sent to CEH (with FAIL status)- %d
        """.formatted(
        date,
        result.getTotalCount(),
        result.getSuccessCount(),
        result.getFailureCount());
  }
}
