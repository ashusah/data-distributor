package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SignalAuditQueryRepositoryAdapterTest {

  @Mock
  private SignalAuditRepository repository;

  private SignalAuditQueryRepositoryAdapter adapter;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    DataDistributorProperties properties = new DataDistributorProperties();
    adapter = new SignalAuditQueryRepositoryAdapter(repository, properties);
  }

  @Test
  void isEventSuccessful_returnsFalseOnNull() {
    assertThat(adapter.isEventSuccessful(null, 1L)).isFalse();
  }

  @Test
  void isEventSuccessful_checksStatus() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus("pass");
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isTrue();
  }

  @Test
  void findFailedEventIdsForDate_returnsEmptyForNullDate() {
    assertThat(adapter.findFailedEventIdsForDate(null)).isEmpty();
  }

  @Test
  void findFailedEventIdsForDate_returnsFailedOnlyLatestPerEvent() {
    SignalAuditJpaEntity latestFail = new SignalAuditJpaEntity();
    latestFail.setUabsEventId(1L);
    latestFail.setStatus("FAIL");
    latestFail.setAuditRecordDateTime(LocalDateTime.now());

    SignalAuditJpaEntity olderPass = new SignalAuditJpaEntity();
    olderPass.setUabsEventId(1L);
    olderPass.setStatus("PASS");
    olderPass.setAuditRecordDateTime(LocalDateTime.now().minusMinutes(1));

    SignalAuditJpaEntity success = new SignalAuditJpaEntity();
    success.setUabsEventId(2L);
    success.setStatus("SUCCESS");
    success.setAuditRecordDateTime(LocalDateTime.now());

    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(List.of(latestFail, olderPass, success));

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).containsExactly(1L);
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void isEventSuccessful_returnsFalseWhenNoAuditEntry() {
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.empty());

    assertThat(adapter.isEventSuccessful(5L, 1L)).isFalse();
  }

  @Test
  void isEventSuccessful_handlesCaseInsensitivePass() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus("pass");
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isTrue();
  }

  @Test
  void isEventSuccessful_handlesCaseInsensitiveSuccess() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus("success");
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isTrue();
  }

  @Test
  void isEventSuccessful_handlesWhitespaceInStatus() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus("  PASS  ");
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isTrue();
  }

  @Test
  void isEventSuccessful_returnsFalseForFailStatus() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus("FAIL");
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isFalse();
  }

  @Test
  void isEventSuccessful_returnsFalseForNullStatus() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus(null);
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isFalse();
  }

  @Test
  void findFailedEventIdsForDate_handlesEmptyAuditList() {
    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(List.of());

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).isEmpty();
  }

  @Test
  void findFailedEventIdsForDate_handlesNullAuditEntities() {
    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(new ArrayList<>(Arrays.asList(null, null)));

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).isEmpty();
  }

  @Test
  void findFailedEventIdsForDate_handlesNullUabsEventId() {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setUabsEventId(null);
    audit.setStatus("FAIL");

    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(List.of(audit));

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).isEmpty();
  }

  @Test
  void findFailedEventIdsForDate_handlesMultipleAuditsForSameEvent() {
    SignalAuditJpaEntity latestFail = new SignalAuditJpaEntity();
    latestFail.setUabsEventId(1L);
    latestFail.setStatus("FAIL");
    latestFail.setAuditRecordDateTime(LocalDateTime.now());

    SignalAuditJpaEntity olderPass = new SignalAuditJpaEntity();
    olderPass.setUabsEventId(1L);
    olderPass.setStatus("PASS");
    olderPass.setAuditRecordDateTime(LocalDateTime.now().minusMinutes(1));

    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(List.of(latestFail, olderPass));

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).containsExactly(1L);
  }

  @Test
  void findFailedEventIdsForDate_handlesNullStatus() {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setUabsEventId(1L);
    audit.setStatus(null);

    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(List.of(audit));

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).containsExactly(1L);
  }

  @Test
  void getLatestAuditStatusForEvent_returnsEmptyWhenNull() {
    assertThat(adapter.getLatestAuditStatusForEvent(null, 1L)).isEmpty();
  }

  @Test
  void getLatestAuditStatusForEvent_returnsEmptyWhenNoAudit() {
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditRecordDateTimeDesc(5L, 1L))
        .thenReturn(Optional.empty());

    assertThat(adapter.getLatestAuditStatusForEvent(5L, 1L)).isEmpty();
  }

  @Test
  void getLatestAuditStatusForEvent_returnsStatusWhenFound() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus("PASS");
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditRecordDateTimeDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.getLatestAuditStatusForEvent(5L, 1L)).contains("PASS");
  }

  @Test
  void isSuccessStatus_handlesNullStatus() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus(null);
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isFalse();
  }

  @Test
  void isSuccessStatus_handlesEmptyStatus() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus("");
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    assertThat(adapter.isEventSuccessful(5L, 1L)).isFalse();
  }

  // ***************************************************
  // NEW TEST- Date- Dec 9
  // ***************************************************

  @Test
  void findFailedEventIdsForDate_handlesNullStatusInAudit() {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setUabsEventId(1L);
    audit.setStatus(null);

    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(List.of(audit));

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).containsExactly(1L);
  }

  @Test
  void getLatestAuditStatusForEvent_handlesNullStatus() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setStatus(null);
    when(repository.findTopByUabsEventIdAndConsumerIdOrderByAuditRecordDateTimeDesc(5L, 1L))
        .thenReturn(Optional.of(entity));

    Optional<String> result = adapter.getLatestAuditStatusForEvent(5L, 1L);
    // When status is null, map() will return Optional.of(null), but Optional.of(null) throws NPE
    // So the adapter will return Optional.empty() if status is null
    // Actually, let me check - map() on Optional.of(entity) with entity.getStatus() == null
    // will return Optional.of(null), which is not allowed. So the adapter might filter it.
    // Let's test that it returns empty when status is null
    assertThat(result).isEmpty();
  }

  // **********************************************************
  // ADDITIONAL TEST
  // **********************************************************

  @Test
  void findFailedEventIdsForDate_preservesOrderingForMultipleFailedEvents() {
    SignalAuditJpaEntity first = new SignalAuditJpaEntity();
    first.setUabsEventId(5L);
    first.setStatus("FAIL");
    first.setAuditRecordDateTime(LocalDateTime.now());

    SignalAuditJpaEntity second = new SignalAuditJpaEntity();
    second.setUabsEventId(3L);
    second.setStatus("FAIL");
    second.setAuditRecordDateTime(LocalDateTime.now().minusSeconds(10));

    when(repository.findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
        any(), any(), any())).thenReturn(List.of(first, second));

    assertThat(adapter.findFailedEventIdsForDate(LocalDate.now())).containsExactly(5L, 3L);
  }
}
