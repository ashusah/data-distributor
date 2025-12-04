package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
}
