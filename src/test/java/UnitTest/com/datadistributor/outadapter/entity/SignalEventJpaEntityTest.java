package com.datadistributor.outadapter.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class SignalEventJpaEntityTest {

  @Test
  void entityGettersAndSettersWork() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setSignalId(10L);
    entity.setSignal(signal);
    entity.setUabsEventId(1L);
    entity.setAgreementId(2L);
    entity.setEventRecordDateTime(LocalDateTime.of(2024, 12, 3, 10, 0));
    entity.setEventType("OVERLIMIT_SIGNAL");
    entity.setEventStatus("PASS");
    entity.setUnauthorizedDebitBalance(500L);
    entity.setBookDate(LocalDate.of(2024, 12, 2));
    ProductRiskMonitoringJpaEntity grv = new ProductRiskMonitoringJpaEntity();
    grv.setGrv((short) 5);
    grv.setProductId((short) 12);
    entity.setGrv(grv);
    entity.setProductId((short) 12);

    assertThat(entity.getSignal().getSignalId()).isEqualTo(10L);
    assertThat(entity.getUabsEventId()).isEqualTo(1L);
    assertThat(entity.getAgreementId()).isEqualTo(2L);
    assertThat(entity.getEventRecordDateTime()).isEqualTo(LocalDateTime.of(2024, 12, 3, 10, 0));
    assertThat(entity.getEventType()).isEqualTo("OVERLIMIT_SIGNAL");
    assertThat(entity.getEventStatus()).isEqualTo("PASS");
    assertThat(entity.getUnauthorizedDebitBalance()).isEqualTo(500L);
    assertThat(entity.getBookDate()).isEqualTo(LocalDate.of(2024, 12, 2));
    assertThat(entity.getGrv()).isEqualTo(grv);
    assertThat(entity.getProductId()).isEqualTo((short) 12);
  }
}
