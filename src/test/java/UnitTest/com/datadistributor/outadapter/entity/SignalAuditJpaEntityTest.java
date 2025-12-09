package com.datadistributor.outadapter.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SignalAuditJpaEntityTest {

  // ***************************************************
  // NEW TEST- Date- Dec 9
  // ***************************************************

  @Test
  void entityCanBeInstantiatedAndSettersWork() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    
    entity.setAuditId(1L);
    entity.setSignalId(2L);
    entity.setUabsEventId(3L);
    entity.setConsumerId(4L);
    entity.setAgreementId(5L);
    entity.setUnauthorizedDebitBalance(100L);
    entity.setStatus("PASS");
    entity.setResponseCode("200");
    entity.setResponseMessage("Success");
    entity.setAuditRecordDateTime(LocalDateTime.now());

    assertThat(entity.getAuditId()).isEqualTo(1L);
    assertThat(entity.getSignalId()).isEqualTo(2L);
    assertThat(entity.getUabsEventId()).isEqualTo(3L);
    assertThat(entity.getConsumerId()).isEqualTo(4L);
    assertThat(entity.getAgreementId()).isEqualTo(5L);
    assertThat(entity.getUnauthorizedDebitBalance()).isEqualTo(100L);
    assertThat(entity.getStatus()).isEqualTo("PASS");
    assertThat(entity.getResponseCode()).isEqualTo("200");
    assertThat(entity.getResponseMessage()).isEqualTo("Success");
    assertThat(entity.getAuditRecordDateTime()).isNotNull();
  }
}

