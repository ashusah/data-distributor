package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventPort;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.report.DeliveryReport;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SignalEventProcessingDomainServiceTest {

  @Mock
  private SignalEventPort signalEventRepository;
  @Mock
  private SignalEventBatchPort signalEventBatchPort;
  @Mock
  private SignalAuditQueryPort signalAuditQueryPort;
  @Mock
  private SignalDispatchSelectorUseCase signalDispatchSelector;
  private JobProgressTracker jobProgressTracker;
  @Mock
  private DeliveryReportPublisher deliveryReportPublisher;

  private SignalEventProcessingDomainService service;
  private LocalDate testDate;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    testDate = LocalDate.of(2024, 12, 3);
    jobProgressTracker = new JobProgressTracker();
    service = new SignalEventProcessingDomainService(
        signalEventRepository,
        signalEventBatchPort,
        signalAuditQueryPort,
        signalDispatchSelector,
        10, // batchSize
        jobProgressTracker,
        deliveryReportPublisher
    );
  }

  @Test
  void processEventsForDate_returnsErrorWhenDateIsNull() {
    JobResult result = service.processEventsForDate("job-1", null);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isZero();
    assertThat(result.getTotalCount()).isZero();
    assertThat(result.getMessage()).isEqualTo("Date is required");
  }

  @Test
  void processEventsForDate_returnsErrorWhenValidationFails() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("FAIL"));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isZero();
    assertThat(result.getTotalCount()).isZero();
    assertThat(result.getMessage()).contains("Prerequisite check failed");
    verify(signalDispatchSelector, never()).selectEventsToSend(any());
  }

  @Test
  void processEventsForDate_continuesWhenNoPriorEvent() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.empty());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    verify(signalDispatchSelector).selectEventsToSend(testDate);
  }

  @Test
  void processEventsForDate_continuesWhenPriorEventHasNoAudit() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.empty());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_continuesWhenPriorEventHasPassStatus() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("PASS"));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_continuesWhenPriorEventHasSuccessStatus() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("SUCCESS"));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_returnsNoEventsWhenSelectorReturnsEmptyAndNoLegacyEvents() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of());
    when(signalEventRepository.countSignalEventsForCEH(testDate))
        .thenReturn(0L);

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getTotalCount()).isZero();
    assertThat(result.getMessage()).contains("No signal events found");
  }

  @Test
  void processEventsForDate_fallsBackToLegacyPagingWhenSelectorReturnsEmpty() {
    SignalEvent event1 = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent event2 = createEvent(2L, 2L, testDate.atTime(11, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of());
    when(signalEventRepository.countSignalEventsForCEH(testDate))
        .thenReturn(2L);
    when(signalEventRepository.getSignalEventsForCEH(testDate, 0, 10))
        .thenReturn(List.of(event1));
    when(signalEventRepository.getSignalEventsForCEH(testDate, 1, 10))
        .thenReturn(List.of(event2));
    when(signalEventRepository.getSignalEventsForCEH(testDate, 2, 10))
        .thenReturn(List.of());
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getTotalCount()).isEqualTo(2);
    verify(signalEventRepository, times(3)).getSignalEventsForCEH(eq(testDate), anyInt(), eq(10));
  }

  @Test
  void processEventsForDate_processesBatchesFromSelector() {
    List<SignalEvent> events = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      events.add(createEvent((long) i, 1L, testDate.atTime(10 + (i % 14), i % 60)));
    }

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(events);
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(10, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(30); // 3 batches * 10 success each
    assertThat(result.getTotalCount()).isEqualTo(25);
    verify(signalEventBatchPort, times(3)).submitBatch(anyList());
  }

  @Test
  void processEventsForDate_handlesMixedSuccessAndFailure() {
    SignalEvent event1 = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent event2 = createEvent(2L, 2L, testDate.atTime(11, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event1, event2));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(
            BatchResult.fromBooleans(List.of(true, false))));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getTotalCount()).isEqualTo(2);
  }

  @Test
  void processEventsForDate_handlesEmptyBatches() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of());
    when(signalEventRepository.countSignalEventsForCEH(testDate))
        .thenReturn(0L);

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getTotalCount()).isZero();
    verify(signalEventBatchPort, never()).submitBatch(anyList());
  }

  @Test
  void processEventsForDate_handlesNullJobId() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate(null, testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_publishesReport() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    service.processEventsForDate("job-1", testDate);

    ArgumentCaptor<DeliveryReport> reportCaptor = ArgumentCaptor.forClass(DeliveryReport.class);
    verify(deliveryReportPublisher).publish(reportCaptor.capture());
    DeliveryReport report = reportCaptor.getValue();
    assertThat(report.getDate()).isEqualTo(testDate);
    assertThat(report.getTotalEvents()).isEqualTo(1);
    assertThat(report.getSuccessEvents()).isEqualTo(1);
    assertThat(report.getFailedEvents()).isZero();
    assertThat(report.getContent()).contains("UABS DELIVERY TO CEH REPORT");
  }

  @Test
  void processEventsForDate_handlesReportPublisherException() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));
    doThrow(new RuntimeException("Publish failed"))
        .when(deliveryReportPublisher).publish(any());

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    verify(deliveryReportPublisher).publish(any());
  }

  @Test
  void processEventsForDate_handlesNullReportPublisher() {
    SignalEventProcessingDomainService serviceWithoutPublisher = new SignalEventProcessingDomainService(
        signalEventRepository,
        signalEventBatchPort,
        signalAuditQueryPort,
        signalDispatchSelector,
        10,
        jobProgressTracker,
        null
    );

    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = serviceWithoutPublisher.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_validatesMultipleEventsWithDifferentPriorStatuses() {
    SignalEvent event1 = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent event2 = createEvent(2L, 2L, testDate.atTime(11, 0));
    SignalEvent prevEvent1 = createEvent(10L, 1L, testDate.minusDays(1).atTime(10, 0));
    SignalEvent prevEvent2 = createEvent(20L, 2L, testDate.minusDays(1).atTime(11, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event1, event2));
    when(signalEventRepository.getPreviousEvent(1L, event1.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent1));
    when(signalEventRepository.getPreviousEvent(2L, event2.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent2));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(10L, 1L))
        .thenReturn(Optional.of("PASS"));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(20L, 1L))
        .thenReturn(Optional.of("FAIL"));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getMessage()).contains("Prerequisite check failed");
    assertThat(result.getMessage()).contains("2"); // event2's uabsEventId
  }

  @Test
  void processEventsForDate_handlesBatchSizeOfOne() {
    SignalEventProcessingDomainService serviceWithBatchSize1 = new SignalEventProcessingDomainService(
        signalEventRepository,
        signalEventBatchPort,
        signalAuditQueryPort,
        signalDispatchSelector,
        1,
        jobProgressTracker,
        deliveryReportPublisher
    );

    SignalEvent event1 = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent event2 = createEvent(2L, 2L, testDate.atTime(11, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event1, event2));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = serviceWithBatchSize1.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(2);
    verify(signalEventBatchPort, times(2)).submitBatch(anyList());
  }

  @Test
  void processEventsForDate_handlesZeroBatchSize() {
    SignalEventProcessingDomainService serviceWithZeroBatchSize = new SignalEventProcessingDomainService(
        signalEventRepository,
        signalEventBatchPort,
        signalAuditQueryPort,
        signalDispatchSelector,
        0, // should be normalized to 1
        jobProgressTracker,
        deliveryReportPublisher
    );

    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = serviceWithZeroBatchSize.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    verify(signalEventBatchPort, times(1)).submitBatch(anyList());
  }

  @Test
  void processEventsForDate_handlesNullEventRecordDateTime() {
    SignalEvent event = createEvent(1L, 1L, null);
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, null))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("PASS"));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_handlesNullUabsEventId() {
    SignalEvent event = createEvent(null, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(null, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(null, 1L))
        .thenReturn(Optional.empty());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_isSuccessStatusHandlesEmptyString() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    // Test when status is empty string (not a success status)
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of(""));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Empty string is not a success status, so prerequisite check should fail
    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getMessage()).contains("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_isSuccessStatusHandlesWhitespace() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("  pass  "));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_isSuccessStatusHandlesCaseInsensitive() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("success"));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  // ***************************************************
  // NEW TEST- Date- Dec 9
  // ***************************************************

  @Test
  void validatePriorEvents_handlesNullEventRecordDateTime() {
    SignalEvent event = createEvent(1L, 1L, null);
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, null))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("PASS"));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void validatePriorEvents_handlesNullUabsEventId() {
    SignalEvent event = createEvent(null, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(null, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(null, 1L))
        .thenReturn(Optional.empty());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void validatePriorEvents_handlesNullPrevEventUabsEventId() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(null, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(null, 1L))
        .thenReturn(Optional.empty());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void isSuccessStatus_handlesNullStatus() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    // Return empty optional instead of null to avoid NPE
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.empty());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Empty optional means no audit entry, so processing should continue
    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  // *****************************
  // FRESH TEST CASE
  // *****************************

  @Test
  void processEventsForDate_usesLegacyPagingWhenSelectorReturnsEmpty() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of());
    when(signalEventRepository.countSignalEventsForCEH(testDate))
        .thenReturn(5L);
    when(signalEventRepository.getSignalEventsForCEH(testDate, 0, 10))
        .thenReturn(List.of(createEvent(1L, 1L, testDate.atTime(10, 0))));
    when(signalEventRepository.getSignalEventsForCEH(testDate, 1, 10))
        .thenReturn(List.of());
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    verify(signalEventRepository).getSignalEventsForCEH(testDate, 0, 10);
  }

  @Test
  void processEventsForDate_returnsEmptyWhenLegacyPagingAlsoEmpty() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of());
    when(signalEventRepository.countSignalEventsForCEH(testDate))
        .thenReturn(0L);

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getTotalCount()).isZero();
    assertThat(result.getMessage()).contains("No signal events found");
  }

  @Test
  void processEventsForDate_handlesMultipleBatchesFromSelector() {
    List<SignalEvent> events = new ArrayList<>();
    for (int i = 1; i <= 25; i++) {
      events.add(createEvent((long) i, 1L, testDate.atTime(10 + (i % 14), i % 60)));
    }

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(events);
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(10, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(30); // 3 batches * 10 success each
    verify(signalEventBatchPort, times(3)).submitBatch(anyList());
  }

  @Test
  void processEventsForDate_handlesMultipleBatchesFromLegacyPaging() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of());
    when(signalEventRepository.countSignalEventsForCEH(testDate))
        .thenReturn(25L);
    when(signalEventRepository.getSignalEventsForCEH(testDate, 0, 10))
        .thenReturn(createEvents(1, 10));
    when(signalEventRepository.getSignalEventsForCEH(testDate, 1, 10))
        .thenReturn(createEvents(11, 10));
    when(signalEventRepository.getSignalEventsForCEH(testDate, 2, 10))
        .thenReturn(createEvents(21, 5));
    when(signalEventRepository.getSignalEventsForCEH(testDate, 3, 10))
        .thenReturn(List.of());
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(10, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(30); // 3 batches
    verify(signalEventBatchPort, times(3)).submitBatch(anyList());
  }

  @Test
  void processEventsForDate_handlesEmptyTrackedBatches() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of());
    when(signalEventRepository.countSignalEventsForCEH(testDate))
        .thenReturn(0L);

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getTotalCount()).isZero();
  }


  @Test
  void processEventsForDate_handlesBatchSizeZero() {
    SignalEventProcessingDomainService serviceWithZeroBatch = new SignalEventProcessingDomainService(
        signalEventRepository,
        signalEventBatchPort,
        signalAuditQueryPort,
        signalDispatchSelector,
        0, // batchSize = 0, should become 1
        jobProgressTracker,
        deliveryReportPublisher
    );

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(createEvent(1L, 1L, testDate.atTime(10, 0))));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = serviceWithZeroBatch.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_handlesBatchSizeNegative() {
    SignalEventProcessingDomainService serviceWithNegativeBatch = new SignalEventProcessingDomainService(
        signalEventRepository,
        signalEventBatchPort,
        signalAuditQueryPort,
        signalDispatchSelector,
        -5, // batchSize = -5, should become 1
        jobProgressTracker,
        deliveryReportPublisher
    );

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(createEvent(1L, 1L, testDate.atTime(10, 0))));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = serviceWithNegativeBatch.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_handlesMixedSuccessAndFailureResults() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(createEvents(1, 20));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(8, 2)))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(7, 3)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(15);
    assertThat(result.getFailureCount()).isEqualTo(5);
    assertThat(result.getTotalCount()).isEqualTo(20L);
  }

  @Test
  void processEventsForDate_publishesReportOnSuccess() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(createEvent(1L, 1L, testDate.atTime(10, 0))));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    ArgumentCaptor<DeliveryReport> reportCaptor = ArgumentCaptor.forClass(DeliveryReport.class);
    verify(deliveryReportPublisher).publish(reportCaptor.capture());
    DeliveryReport report = reportCaptor.getValue();
    assertThat(report.getDate()).isEqualTo(testDate);
    assertThat(report.getTotalEvents()).isEqualTo(1L);
    assertThat(report.getSuccessEvents()).isEqualTo(1);
    assertThat(report.getFailedEvents()).isZero();
    assertThat(report.getContent()).contains("2024-12-03");
    assertThat(report.getContent()).contains("1");
  }

  @Test
  void processEventsForDate_handlesReportPublishingFailure() {
    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(createEvent(1L, 1L, testDate.atTime(10, 0))));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));
    doThrow(new RuntimeException("Publish failed"))
        .when(deliveryReportPublisher).publish(any(DeliveryReport.class));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should still return success even if report publishing fails
    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_handlesNullDeliveryReportPublisher() {
    SignalEventProcessingDomainService serviceWithoutPublisher = new SignalEventProcessingDomainService(
        signalEventRepository,
        signalEventBatchPort,
        signalAuditQueryPort,
        signalDispatchSelector,
        10,
        jobProgressTracker,
        null // no publisher
    );

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(createEvent(1L, 1L, testDate.atTime(10, 0))));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = serviceWithoutPublisher.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    // Should not throw NPE
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithNullUabsEventId() {
    SignalEvent event = createEvent(null, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(null, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(null, 1L))
        .thenReturn(Optional.empty());

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should continue processing when prevEvent has null uabsEventId
    assertThat(result.getMessage()).doesNotContain("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithNullPrevEventUabsEventId() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(null, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(null, 1L))
        .thenReturn(Optional.empty());

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should continue processing when prevEvent has null uabsEventId
    assertThat(result.getMessage()).doesNotContain("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithNullStatus() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    // Return empty optional to simulate no status (which means continue processing)
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.empty());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // No status means continue processing
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getMessage()).doesNotContain("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithEmptyStringStatus() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("")); // empty status

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Empty status should be treated as FAIL
    assertThat(result.getMessage()).contains("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithWhitespaceStatus() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("   ")); // whitespace status

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Whitespace status should be treated as FAIL
    assertThat(result.getMessage()).contains("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithSuccessStatus() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("PASS"));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should proceed when status is PASS
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getMessage()).doesNotContain("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithSuccessStatusCaseInsensitive() {
    SignalEvent event = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, event.getEventRecordDateTime()))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("success")); // lowercase
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should proceed when status is "success" (case insensitive)
    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithMultipleEventsSorted() {
    SignalEvent event1 = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent event2 = createEvent(2L, 2L, testDate.atTime(11, 0));
    SignalEvent prev1 = createEvent(10L, 1L, testDate.minusDays(1).atTime(10, 0));
    SignalEvent prev2 = createEvent(20L, 2L, testDate.minusDays(1).atTime(11, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event2, event1)); // unsorted
    when(signalEventRepository.getPreviousEvent(1L, event1.getEventRecordDateTime()))
        .thenReturn(Optional.of(prev1));
    when(signalEventRepository.getPreviousEvent(2L, event2.getEventRecordDateTime()))
        .thenReturn(Optional.of(prev2));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(10L, 1L))
        .thenReturn(Optional.of("PASS"));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(20L, 1L))
        .thenReturn(Optional.of("FAIL"));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should detect failure for event2
    assertThat(result.getMessage()).contains("uabsEventIds=[2]");
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithNullEventRecordDateTime() {
    SignalEvent event = createEvent(1L, 1L, null);
    SignalEvent prevEvent = createEvent(2L, 1L, testDate.minusDays(1).atTime(10, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event));
    when(signalEventRepository.getPreviousEvent(1L, null))
        .thenReturn(Optional.of(prevEvent));
    when(signalAuditQueryPort.getLatestAuditStatusForEvent(2L, 1L))
        .thenReturn(Optional.of("PASS"));
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(List.of(event));
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(1, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should handle null eventRecordDateTime in sorting
    assertThat(result.getSuccessCount()).isEqualTo(1);
  }

  @Test
  void processEventsForDate_handlesValidatePriorEventsWithNullUabsEventIdInSorting() {
    SignalEvent event1 = createEvent(1L, 1L, testDate.atTime(10, 0));
    SignalEvent event2 = createEvent(null, 1L, testDate.atTime(11, 0));

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of(event1, event2));
    when(signalEventRepository.getPreviousEvent(any(), any()))
        .thenReturn(Optional.empty());

    JobResult result = service.processEventsForDate("job-1", testDate);

    // Should handle null uabsEventId in sorting
    assertThat(result.getMessage()).doesNotContain("Prerequisite check failed");
  }

  @Test
  void processEventsForDate_handlesSingleBatchExactlyBatchSize() {
    List<SignalEvent> events = new ArrayList<>();
    for (int i = 1; i <= 10; i++) {
      events.add(createEvent((long) i, 1L, testDate.atTime(10, 0)));
    }

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(events);
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(10, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(10);
    verify(signalEventBatchPort, times(1)).submitBatch(anyList());
  }

  @Test
  void processEventsForDate_handlesSingleBatchOneMoreThanBatchSize() {
    List<SignalEvent> events = new ArrayList<>();
    for (int i = 1; i <= 11; i++) {
      events.add(createEvent((long) i, 1L, testDate.atTime(10, 0)));
    }

    when(signalEventRepository.getAllSignalEventsOfThisDate(testDate))
        .thenReturn(List.of());
    when(signalDispatchSelector.selectEventsToSend(testDate))
        .thenReturn(events);
    when(signalEventBatchPort.submitBatch(anyList()))
        .thenReturn(CompletableFuture.completedFuture(new BatchResult(10, 0)));

    JobResult result = service.processEventsForDate("job-1", testDate);

    assertThat(result.getSuccessCount()).isEqualTo(20); // 2 batches
    verify(signalEventBatchPort, times(2)).submitBatch(anyList());
  }

  private List<SignalEvent> createEvents(int start, int count) {
    List<SignalEvent> events = new ArrayList<>();
    for (int i = start; i < start + count; i++) {
      events.add(createEvent((long) i, 1L, testDate.atTime(10, 0)));
    }
    return events;
  }

  private SignalEvent createEvent(Long uabsEventId, Long signalId, LocalDateTime eventRecordDateTime) {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(uabsEventId);
    event.setSignalId(signalId);
    event.setEventRecordDateTime(eventRecordDateTime);
    event.setAgreementId(100L);
    event.setEventStatus("OVERLIMIT_SIGNAL");
    return event;
  }
}

