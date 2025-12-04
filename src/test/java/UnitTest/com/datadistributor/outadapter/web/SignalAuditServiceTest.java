package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
}
