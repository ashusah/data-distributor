package com.datadistributor.domain.service;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.report.DeliveryReport;
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

@Slf4j
public class SignalEventProcessingService implements SignalEventProcessingUseCase {

  private final SignalEventRepository signalEventRepository;
  private final SignalEventBatchPort signalEventBatchPort;
  private final SignalAuditQueryPort signalAuditQueryPort;
  private final int batchSize;
  private final JobProgressTracker jobProgressTracker;
  private final DeliveryReportPublisher deliveryReportPublisher;

  public SignalEventProcessingService(SignalEventRepository signalEventRepository,
                                      SignalEventBatchPort signalEventBatchPort,
                                      SignalAuditQueryPort signalAuditQueryPort,
                                      int batchSize,
                                      JobProgressTracker jobProgressTracker,
                                      DeliveryReportPublisher deliveryReportPublisher) {
    this.signalEventRepository = signalEventRepository;
    this.signalEventBatchPort = signalEventBatchPort;
    this.signalAuditQueryPort = signalAuditQueryPort;
    this.batchSize = Math.max(1, batchSize);
    this.jobProgressTracker = jobProgressTracker;
    this.deliveryReportPublisher = deliveryReportPublisher;
  }

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
    long totalCount = signalEventRepository.countSignalEventsForCEH(date);
    if (totalCount == 0) {
      return new JobResult(0, 0, 0, "No signal events found for date " + date);
    }

    int totalBatches = (int) Math.ceil((double) totalCount / batchSize);
    log.info("🚀 Starting processing for {} events on {} (~{} batches)", totalCount, date, totalBatches);
    List<TrackedBatch> trackedBatches = new ArrayList<>();
    AtomicInteger batchCounter = new AtomicInteger();

    JobProgressTracker.JobProgress progress =
        jobProgressTracker.start(Optional.ofNullable(jobId).orElse(null), totalBatches);

    int page = 0;
    while (true) {
      List<SignalEvent> chunk = signalEventRepository.getSignalEventsForCEH(date, page, batchSize);
      if (chunk.isEmpty()) {
        break;
      }
      trackedBatches.add(submitBatch(new ArrayList<>(chunk), batchCounter.incrementAndGet()));
      page++;
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
