package com.datadistributor.domain.service;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventRetryUseCase;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignalEventRetryService implements SignalEventRetryUseCase {

  private final SignalAuditQueryPort signalAuditQueryPort;
  private final SignalEventRepository signalEventRepository;
  private final SignalEventBatchPort signalEventBatchPort;
  private final JobProgressTracker jobProgressTracker;
  private final int batchSize;

  public SignalEventRetryService(SignalAuditQueryPort signalAuditQueryPort,
                                 SignalEventRepository signalEventRepository,
                                 SignalEventBatchPort signalEventBatchPort,
                                 JobProgressTracker jobProgressTracker,
                                 int batchSize) {
    this.signalAuditQueryPort = signalAuditQueryPort;
    this.signalEventRepository = signalEventRepository;
    this.signalEventBatchPort = signalEventBatchPort;
    this.jobProgressTracker = jobProgressTracker;
    this.batchSize = Math.max(1, batchSize);
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

    int totalCount = toSend.size();
    int totalBatches = (int) Math.ceil((double) totalCount / batchSize);
    String resolvedJobId = Optional.ofNullable(jobId).orElse("retry-" + UUID.randomUUID());

    log.info("🔁 Starting retry job={} for date {} | events={} (~{} batches)", resolvedJobId, date, totalCount, totalBatches);

    List<TrackedBatch> trackedBatches = new ArrayList<>();
    AtomicInteger batchCounter = new AtomicInteger();
    JobProgressTracker.JobProgress progress = jobProgressTracker.start(resolvedJobId, totalBatches);

    List<List<SignalEvent>> chunks = chunk(toSend, batchSize);
    for (List<SignalEvent> chunk : chunks) {
      trackedBatches.add(submitBatch(new ArrayList<>(chunk), batchCounter.incrementAndGet()));
    }

    attachProgressLogging(progress, trackedBatches);
    JobResult result = awaitJobCompletion(trackedBatches, "Retry complete for " + date, totalCount);

    int combinedFailure = result.getFailureCount() + missing;
    JobResult combined = new JobResult(
        result.getSuccessCount(),
        combinedFailure,
        distinctIds.size(),
        result.getMessage() + (missing > 0 ? (" | missing=" + missing) : ""));

    log.info("🔁 Retry job={} finished for {} success={} failure={} (missing={})",
        resolvedJobId, date, combined.getSuccessCount(), combined.getFailureCount(), missing);
    return combined;
  }

  private TrackedBatch submitBatch(List<SignalEvent> batch, int batchNumber) {
    String ids = batch.stream()
        .map(SignalEvent::getUabsEventId)
        .map(String::valueOf)
        .collect(Collectors.joining(","));
    log.info("Retry submitting batch #{} (size {}) | uabsEventIds=[{}]", batchNumber, batch.size(), ids);
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
        .toList();

    CompletableFuture<Void> all = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    all.join();

    List<BatchResult> results = futures.stream()
        .map(CompletableFuture::join)
        .toList();

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
}
