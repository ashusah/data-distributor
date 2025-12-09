package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventPort;
import com.datadistributor.domain.outport.SignalEventSenderPort;
import com.datadistributor.domain.service.SignalEventRetryDomainService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SignalEventRetryServiceTest {

  private FakeAuditPort auditPort;
  private FakeEventRepository eventRepository;
  private FakeSenderPort senderPort;
  private SignalEventRetryDomainService service;

  @BeforeEach
  void setUp() {
    auditPort = new FakeAuditPort();
    eventRepository = new FakeEventRepository();
    senderPort = new FakeSenderPort();
    service = new SignalEventRetryDomainService(
        auditPort,
        eventRepository,
        senderPort
    );
  }

  @Test
  void returnsNoWorkWhenNoFailures() {
    JobResult result = service.retryFailedEvents("job-1", LocalDate.of(2024, 12, 3));

    assertThat(result.getTotalCount()).isZero();
    assertThat(senderPort.sent).isEmpty();
  }

  @Test
  void retriesFailedEventsAndBatchesThem() {
    auditPort.failedIds = List.of(1L, 2L, 3L);
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));
    eventRepository.save(event(2L, LocalDateTime.of(2024, 12, 3, 11, 0)));
    eventRepository.save(event(3L, LocalDateTime.of(2024, 12, 3, 12, 0)));

    JobResult result = service.retryFailedEvents("job-2", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isEqualTo(3);
    assertThat(result.getFailureCount()).isZero();
    assertThat(result.getTotalCount()).isEqualTo(3);
    assertThat(senderPort.sent).containsExactly(1L, 2L, 3L);
  }

  @Test
  void countsMissingEventsAsFailure() {
    auditPort.failedIds = List.of(1L, 2L);
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));

    JobResult result = service.retryFailedEvents("job-3", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getTotalCount()).isEqualTo(2);
  }

  private SignalEvent event(long id, LocalDateTime timestamp) {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(id);
    event.setSignalId(10L);
    event.setAgreementId(100L);
    event.setEventRecordDateTime(timestamp);
    event.setEventStatus("FAILURE_CASE");
    return event;
  }

  private static class FakeAuditPort implements SignalAuditQueryPort {
    List<Long> failedIds = Collections.emptyList();

    @Override
    public boolean isEventSuccessful(Long uabsEventId, long consumerId) {
      return false;
    }

    @Override
    public List<Long> findFailedEventIdsForDate(LocalDate date) {
      return failedIds == null ? Collections.emptyList() : failedIds;
    }

    @Override
    public Optional<String> getLatestAuditStatusForEvent(Long uabsEventId, long consumerId) {
      return Optional.empty();
    }
  }

  private static class FakeEventRepository implements SignalEventPort {
    private final Map<Long, SignalEvent> events = new HashMap<>();

    void save(SignalEvent event) {
      events.put(event.getUabsEventId(), event);
    }

    @Override
    public List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) { return List.of(); }
    @Override
    public List<SignalEvent> getSignalEventsForCEH(LocalDate date, int page, int size) { return List.of(); }
    @Override
    public long countSignalEventsForCEH(LocalDate date) { return 0; }
    @Override
    public java.util.Optional<SignalEvent> getPreviousEvent(Long signalId, LocalDateTime before) { return java.util.Optional.empty(); }
    @Override
    public java.util.Optional<SignalEvent> getEarliestOverlimitEvent(Long signalId) { return java.util.Optional.empty(); }
    @Override
    public List<SignalEvent> findByUabsEventIdIn(List<Long> uabsEventIds) {
      return uabsEventIds.stream()
          .map(events::get)
          .filter(e -> e != null)
          .toList();
    }
  }

  private static class FakeSenderPort implements SignalEventSenderPort {
    final List<Long> sent = new ArrayList<>();
    boolean shouldFail = false;

    @Override
    public boolean send(SignalEvent event) {
      if (shouldFail) {
        return false;
      }
      sent.add(event.getUabsEventId());
      return true;
    }
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void returnsErrorWhenDateIsNull() {
    JobResult result = service.retryFailedEvents("job-1", null);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isZero();
    assertThat(result.getTotalCount()).isZero();
    assertThat(result.getMessage()).isEqualTo("Date is required");
  }

  @Test
  void handlesNullFailedIds() {
    auditPort.failedIds = null;
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));

    JobResult result = service.retryFailedEvents("job-4", LocalDate.of(2024, 12, 3));

    assertThat(result.getTotalCount()).isZero();
  }

  @Test
  void filtersNullIdsFromFailedList() {
    auditPort.failedIds = new ArrayList<>(Arrays.asList(1L, null, 2L, null));
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));
    eventRepository.save(event(2L, LocalDateTime.of(2024, 12, 3, 11, 0)));

    JobResult result = service.retryFailedEvents("job-5", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getTotalCount()).isEqualTo(2);
  }

  @Test
  void handlesDuplicateFailedIds() {
    auditPort.failedIds = List.of(1L, 1L, 2L, 2L);
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));
    eventRepository.save(event(2L, LocalDateTime.of(2024, 12, 3, 11, 0)));

    JobResult result = service.retryFailedEvents("job-6", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getTotalCount()).isEqualTo(2);
  }

  @Test
  void handlesEventsWithNullUabsEventId() {
    auditPort.failedIds = List.of(1L, 2L);
    SignalEvent event1 = event(1L, LocalDateTime.of(2024, 12, 3, 10, 0));
    SignalEvent event2 = event(2L, LocalDateTime.of(2024, 12, 3, 11, 0));
    event2.setUabsEventId(null);
    eventRepository.save(event1);
    eventRepository.save(event2);

    JobResult result = service.retryFailedEvents("job-7", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(result.getTotalCount()).isEqualTo(2);
  }

  @Test
  void sortsEventsByDateTimeThenUabsEventId() {
    auditPort.failedIds = List.of(3L, 1L, 2L);
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 12, 0)));
    eventRepository.save(event(2L, LocalDateTime.of(2024, 12, 3, 11, 0)));
    eventRepository.save(event(3L, LocalDateTime.of(2024, 12, 3, 10, 0)));

    JobResult result = service.retryFailedEvents("job-8", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isEqualTo(3);
    assertThat(senderPort.sent).containsExactly(3L, 2L, 1L);
  }

  @Test
  void handlesNullEventRecordDateTime() {
    auditPort.failedIds = List.of(1L, 2L);
    SignalEvent event1 = event(1L, null);
    SignalEvent event2 = event(2L, LocalDateTime.of(2024, 12, 3, 11, 0));
    eventRepository.save(event1);
    eventRepository.save(event2);

    JobResult result = service.retryFailedEvents("job-9", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isEqualTo(2);
  }

  @Test
  void handlesSendFailure() {
    auditPort.failedIds = List.of(1L);
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));
    senderPort.shouldFail = true;

    JobResult result = service.retryFailedEvents("job-10", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isEqualTo(1);
  }

  @Test
  void includesMissingCountInMessage() {
    auditPort.failedIds = List.of(1L, 2L, 3L);
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));

    JobResult result = service.retryFailedEvents("job-11", LocalDate.of(2024, 12, 3));

    assertThat(result.getMessage()).contains("missing=2");
  }

  @Test
  void handlesAllEventsMissing() {
    auditPort.failedIds = List.of(1L, 2L, 3L);

    JobResult result = service.retryFailedEvents("job-12", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isEqualTo(3);
    assertThat(result.getTotalCount()).isEqualTo(3);
    assertThat(result.getMessage()).contains("no matching signal events");
  }

  @Test
  void handlesMixedSuccessAndFailure() {
    auditPort.failedIds = List.of(1L, 2L, 3L);
    eventRepository.save(event(1L, LocalDateTime.of(2024, 12, 3, 10, 0)));
    eventRepository.save(event(2L, LocalDateTime.of(2024, 12, 3, 11, 0)));
    senderPort.shouldFail = true;

    JobResult result = service.retryFailedEvents("job-13", LocalDate.of(2024, 12, 3));

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isEqualTo(3);
  }
}
