package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SignalAuditServiceTest {

  @Mock
  private SignalAuditRepository repository;

  private SignalAuditService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAudit().setConsumerId(42L);
    service = new SignalAuditService(repository, properties);
  }

  @Test
  void persistAudit_mapsFields() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);
    event.setAgreementId(3L);
    event.setUnauthorizedDebitBalance(4L);

    service.persistAudit(event, "PASS", "200", "ok");

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    SignalAuditJpaEntity saved = captor.getValue();
    assertThat(saved.getSignalId()).isEqualTo(1L);
    assertThat(saved.getUabsEventId()).isEqualTo(2L);
    assertThat(saved.getAgreementId()).isEqualTo(3L);
    assertThat(saved.getUnauthorizedDebitBalance()).isEqualTo(4L);
    assertThat(saved.getConsumerId()).isEqualTo(42L);
    assertThat(saved.getStatus()).isEqualTo("PASS");
    assertThat(saved.getResponseCode()).isEqualTo("200");
    assertThat(saved.getResponseMessage()).isEqualTo("ok");
    assertThat(saved.getAuditRecordDateTime()).isBeforeOrEqualTo(LocalDateTime.now());
  }

  @Test
  void persistAudit_handlesNullAmounts() {
    service.persistAudit(new SignalEvent(), "PASS", "200", "msg");
    verify(repository).save(any(SignalAuditJpaEntity.class));
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void persistAudit_truncatesLongResponseCode() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);
    event.setAgreementId(3L);

    service.persistAudit(event, "PASS", "12345678901234567890", "msg");

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getResponseCode().length()).isLessThanOrEqualTo(10);
  }

  @Test
  void persistAudit_truncatesLongResponseMessage() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);
    event.setAgreementId(3L);
    String longMessage = "a".repeat(200);

    service.persistAudit(event, "PASS", "200", longMessage);

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getResponseMessage().length()).isLessThanOrEqualTo(100);
  }

  @Test
  void persistAudit_handlesNullResponseCode() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);

    service.persistAudit(event, "PASS", null, "msg");

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getResponseCode()).isEmpty();
  }

  @Test
  void persistAudit_handlesNullResponseMessage() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);

    service.persistAudit(event, "PASS", "200", null);

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getResponseMessage()).isEmpty();
  }

  @Test
  void persistAudit_handlesNullUnauthorizedDebitBalance() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);
    event.setAgreementId(3L);
    event.setUnauthorizedDebitBalance(null);

    service.persistAudit(event, "PASS", "200", "msg");

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getUnauthorizedDebitBalance()).isEqualTo(0L);
  }

  @Test
  void persistAudit_handlesZeroUnauthorizedDebitBalance() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);
    event.setAgreementId(3L);
    event.setUnauthorizedDebitBalance(0L);

    service.persistAudit(event, "PASS", "200", "msg");

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getUnauthorizedDebitBalance()).isEqualTo(0L);
  }

  @Test
  void persistAudit_handlesNonZeroUnauthorizedDebitBalance() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);
    event.setAgreementId(3L);
    event.setUnauthorizedDebitBalance(500L);

    service.persistAudit(event, "PASS", "200", "msg");

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getUnauthorizedDebitBalance()).isEqualTo(500L);
  }

  @Test
  void logAuditFailure_handlesNullEvent() {
    // Test that null event is handled gracefully (won't crash, just logs)
    service.logAuditFailure(null, new RuntimeException("error"));

    // Method logs error but doesn't save when event is null
    verify(repository, never()).save(any());
  }

  @Test
  void logAuditFailure_handlesEventWithNullUabsEventId() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(null);

    service.logAuditFailure(event, new RuntimeException("error"));

    verify(repository, never()).save(any());
  }

  @Test
  void truncate_handlesNullValue() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);

    service.persistAudit(event, "PASS", null, null);

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getResponseCode()).isEmpty();
    assertThat(captor.getValue().getResponseMessage()).isEmpty();
  }

  @Test
  void truncate_handlesExactLength() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(1L);
    event.setUabsEventId(2L);
    String exactCode = "a".repeat(10);
    String exactMessage = "b".repeat(100);

    service.persistAudit(event, "PASS", exactCode, exactMessage);

    ArgumentCaptor<SignalAuditJpaEntity> captor = ArgumentCaptor.forClass(SignalAuditJpaEntity.class);
    verify(repository).save(captor.capture());
    assertThat(captor.getValue().getResponseCode()).isEqualTo(exactCode);
    assertThat(captor.getValue().getResponseMessage()).isEqualTo(exactMessage);
  }
}
