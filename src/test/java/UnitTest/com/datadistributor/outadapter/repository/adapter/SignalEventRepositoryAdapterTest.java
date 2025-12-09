package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageRequest;

class SignalEventRepositoryAdapterTest {

  @Mock
  private SignalEventJpaRepository jpaRepository;

  @Mock
  private SignalEventMapper mapper;

  private SignalEventRepositoryAdapter adapter;

  private final DataDistributorProperties properties = new DataDistributorProperties();

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties.getProcessing().setMinUnauthorizedDebitBalance(250);
    adapter = new SignalEventRepositoryAdapter(jpaRepository, mapper, properties);
  }

  @Test
  void getAllSignalEventsOfThisDate_queriesDayRange() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    when(jpaRepository.findByEventRecordDateTimeBetween(any(), any())).thenReturn(List.of(entity));
    SignalEvent mapped = new SignalEvent();
    mapped.setUabsEventId(1L);
    when(mapper.toDomainList(List.of(entity))).thenReturn(List.of(mapped));

    List<SignalEvent> result = adapter.getAllSignalEventsOfThisDate(date);

    ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(jpaRepository).findByEventRecordDateTimeBetween(start.capture(), end.capture());
    assertThat(start.getValue()).isEqualTo(date.atStartOfDay());
    assertThat(end.getValue()).isEqualTo(date.atTime(LocalTime.MAX));
    assertThat(result).containsExactly(mapped);
  }

  @Test
  void getSignalEventsForCEH_appliesFilters() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    when(jpaRepository.findPageForCEH(any(), any(), eq(250L), eq(PageRequest.of(1, 10))))
        .thenReturn(List.of(entity));
    SignalEvent mapped = new SignalEvent();
    when(mapper.toDomainList(List.of(entity))).thenReturn(List.of(mapped));

    List<SignalEvent> result = adapter.getSignalEventsForCEH(date, 1, 10);

    // Verify the query is called with correct parameters
    ArgumentCaptor<LocalDateTime> start = ArgumentCaptor.forClass(LocalDateTime.class);
    ArgumentCaptor<LocalDateTime> end = ArgumentCaptor.forClass(LocalDateTime.class);
    verify(jpaRepository).findPageForCEH(start.capture(), end.capture(), eq(250L), eq(PageRequest.of(1, 10)));
    assertThat(start.getValue()).isEqualTo(date.atStartOfDay());
    assertThat(end.getValue()).isEqualTo(date.atTime(LocalTime.MAX));
    assertThat(result).containsExactly(mapped);
  }

  @Test
  void countSignalEventsForCEH_usesLookback() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    when(jpaRepository.countEligibleForCEH(any(), any(), eq(250L)))
        .thenReturn(7L);

    assertThat(adapter.countSignalEventsForCEH(date)).isEqualTo(7L);
  }

  @Test
  void getPreviousEvent_returnsEmptyOnNullInput() {
    assertThat(adapter.getPreviousEvent(null, LocalDateTime.now())).isEmpty();
    assertThat(adapter.getPreviousEvent(1L, null)).isEmpty();
  }

  @Test
  void getPreviousEvent_returnsMappedFirst() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    when(jpaRepository.findPreviousEvent(eq(9L), any(), eq(PageRequest.of(0, 1))))
        .thenReturn(List.of(entity));
    SignalEvent mapped = new SignalEvent();
    mapped.setUabsEventId(9L);
    when(mapper.toDomain(entity)).thenReturn(mapped);

    assertThat(adapter.getPreviousEvent(9L, LocalDateTime.now())).contains(mapped);
  }

  @Test
  void getEarliestOverlimitEvent_handlesNull() {
    assertThat(adapter.getEarliestOverlimitEvent(null)).isEmpty();
  }

  @Test
  void getEarliestOverlimitEvent_mapsResult() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    when(jpaRepository.findBySignalIdAndEventStatusOrderByEventRecordDateTimeAsc(10L, "OVERLIMIT_SIGNAL"))
        .thenReturn(List.of(entity));
    SignalEvent mapped = new SignalEvent();
    when(mapper.toDomain(entity)).thenReturn(mapped);

    assertThat(adapter.getEarliestOverlimitEvent(10L)).contains(mapped);
  }

  @Test
  void findByUabsEventIdIn_returnsEmptyForNullOrEmptyInput() {
    assertThat(adapter.findByUabsEventIdIn(null)).isEmpty();
    assertThat(adapter.findByUabsEventIdIn(List.of())).isEmpty();
  }

  @Test
  void findByUabsEventIdIn_returnsOrderedByInput() {
    SignalEventJpaEntity first = new SignalEventJpaEntity();
    first.setUabsEventId(2L);
    SignalEventJpaEntity second = new SignalEventJpaEntity();
    second.setUabsEventId(1L);
    when(jpaRepository.findByUabsEventIdIn(List.of(1L, 2L))).thenReturn(List.of(first, second));

    SignalEvent mapped1 = new SignalEvent();
    mapped1.setUabsEventId(2L);
    SignalEvent mapped2 = new SignalEvent();
    mapped2.setUabsEventId(1L);
    when(mapper.toDomain(first)).thenReturn(mapped1);
    when(mapper.toDomain(second)).thenReturn(mapped2);

    assertThat(adapter.findByUabsEventIdIn(List.of(1L, 2L)))
        .extracting(SignalEvent::getUabsEventId)
        .containsExactly(1L, 2L);
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void getPreviousEvent_returnsEmptyWhenNoResults() {
    when(jpaRepository.findPreviousEvent(eq(9L), any(), eq(PageRequest.of(0, 1))))
        .thenReturn(List.of());

    assertThat(adapter.getPreviousEvent(9L, LocalDateTime.now())).isEmpty();
  }

  @Test
  void getEarliestOverlimitEvent_returnsEmptyWhenNoResults() {
    when(jpaRepository.findBySignalIdAndEventStatusOrderByEventRecordDateTimeAsc(10L, "OVERLIMIT_SIGNAL"))
        .thenReturn(List.of());

    assertThat(adapter.getEarliestOverlimitEvent(10L)).isEmpty();
  }

  @Test
  void findByUabsEventIdIn_handlesDuplicateIds() {
    SignalEventJpaEntity entity1 = new SignalEventJpaEntity();
    entity1.setUabsEventId(1L);
    SignalEventJpaEntity entity2 = new SignalEventJpaEntity();
    entity2.setUabsEventId(1L);
    when(jpaRepository.findByUabsEventIdIn(List.of(1L, 1L))).thenReturn(List.of(entity1, entity2));

    SignalEvent mapped1 = new SignalEvent();
    mapped1.setUabsEventId(1L);
    when(mapper.toDomain(entity1)).thenReturn(mapped1);
    when(mapper.toDomain(entity2)).thenReturn(mapped1);

    List<SignalEvent> result = adapter.findByUabsEventIdIn(List.of(1L, 1L));

    assertThat(result).hasSize(2);
    assertThat(result).extracting(SignalEvent::getUabsEventId).containsExactly(1L, 1L);
  }

  @Test
  void findByUabsEventIdIn_handlesNullMappedEvents() {
    SignalEventJpaEntity entity1 = new SignalEventJpaEntity();
    entity1.setUabsEventId(1L);
    SignalEventJpaEntity entity2 = new SignalEventJpaEntity();
    entity2.setUabsEventId(2L);
    when(jpaRepository.findByUabsEventIdIn(List.of(1L, 2L))).thenReturn(List.of(entity1, entity2));

    SignalEvent mapped1 = new SignalEvent();
    mapped1.setUabsEventId(1L);
    when(mapper.toDomain(entity1)).thenReturn(mapped1);
    when(mapper.toDomain(entity2)).thenReturn(null);

    List<SignalEvent> result = adapter.findByUabsEventIdIn(List.of(1L, 2L));

    assertThat(result).hasSize(1);
    assertThat(result).extracting(SignalEvent::getUabsEventId).containsExactly(1L);
  }

  @Test
  void findByUabsEventIdIn_handlesMissingEvents() {
    SignalEventJpaEntity entity1 = new SignalEventJpaEntity();
    entity1.setUabsEventId(1L);
    when(jpaRepository.findByUabsEventIdIn(List.of(1L, 2L, 3L))).thenReturn(List.of(entity1));

    SignalEvent mapped1 = new SignalEvent();
    mapped1.setUabsEventId(1L);
    when(mapper.toDomain(entity1)).thenReturn(mapped1);

    List<SignalEvent> result = adapter.findByUabsEventIdIn(List.of(1L, 2L, 3L));

    assertThat(result).hasSize(1);
    assertThat(result).extracting(SignalEvent::getUabsEventId).containsExactly(1L);
  }

  @Test
  void findByUabsEventIdIn_handlesNullUabsEventIdInDomain() {
    SignalEventJpaEntity entity1 = new SignalEventJpaEntity();
    entity1.setUabsEventId(1L);
    when(jpaRepository.findByUabsEventIdIn(List.of(1L))).thenReturn(List.of(entity1));

    SignalEvent mapped1 = new SignalEvent();
    mapped1.setUabsEventId(null);
    when(mapper.toDomain(entity1)).thenReturn(mapped1);

    List<SignalEvent> result = adapter.findByUabsEventIdIn(List.of(1L));

    assertThat(result).isEmpty();
  }

  // ***************************************************
  // NEW TEST- Date- Dec 9
  // ***************************************************

  @Test
  void getPreviousEvent_handlesNullMappedEvent() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    when(jpaRepository.findPreviousEvent(eq(9L), any(), eq(PageRequest.of(0, 1))))
        .thenReturn(List.of(entity));
    when(mapper.toDomain(entity)).thenReturn(null);

    assertThat(adapter.getPreviousEvent(9L, LocalDateTime.now())).isEmpty();
  }

  @Test
  void getEarliestOverlimitEvent_handlesNullMappedEvent() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    when(jpaRepository.findBySignalIdAndEventStatusOrderByEventRecordDateTimeAsc(10L, "OVERLIMIT_SIGNAL"))
        .thenReturn(List.of(entity));
    when(mapper.toDomain(entity)).thenReturn(null);

    assertThat(adapter.getEarliestOverlimitEvent(10L)).isEmpty();
  }
}
