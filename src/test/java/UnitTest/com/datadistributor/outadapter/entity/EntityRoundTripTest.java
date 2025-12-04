package com.datadistributor.outadapter.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;

class EntityRoundTripTest {

  @Test
  void signalEventJpaEntity_gettersAndSetters() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setUabsEventId(1L);
    entity.setSignalId(2L);
    entity.setAgreementId(3L);
    entity.setEventRecordDateTime(LocalDateTime.of(2024, 1, 1, 1, 0));
    entity.setEventType("TYPE");
    entity.setEventStatus("STATUS");
    entity.setUnauthorizedDebitBalance(4L);
    entity.setBookDate(LocalDate.of(2024, 1, 2));
    entity.setGrv((short) 5);
    entity.setProductId((short) 6);

    assertThat(entity.getUabsEventId()).isEqualTo(1L);
    assertThat(entity.getSignalId()).isEqualTo(2L);
    assertThat(entity.getAgreementId()).isEqualTo(3L);
    assertThat(entity.getEventRecordDateTime()).isEqualTo(LocalDateTime.of(2024, 1, 1, 1, 0));
    assertThat(entity.getEventType()).isEqualTo("TYPE");
    assertThat(entity.getEventStatus()).isEqualTo("STATUS");
    assertThat(entity.getUnauthorizedDebitBalance()).isEqualTo(4L);
    assertThat(entity.getBookDate()).isEqualTo(LocalDate.of(2024, 1, 2));
    assertThat(entity.getGrv()).isEqualTo((short) 5);
    assertThat(entity.getProductId()).isEqualTo((short) 6);
  }

  @Test
  void signalAuditJpaEntity_gettersAndSetters() {
    SignalAuditJpaEntity entity = new SignalAuditJpaEntity();
    entity.setAuditId(10L);
    entity.setSignalId(11L);
    entity.setUabsEventId(12L);
    entity.setConsumerId(13L);
    entity.setAgreementId(14L);
    entity.setUnauthorizedDebitBalance(15L);
    entity.setStatus("PASS");
    entity.setResponseCode("200");
    entity.setResponseMessage("ok");
    LocalDateTime now = LocalDateTime.now();
    entity.setAuditRecordDateTime(now);

    assertThat(entity.getAuditId()).isEqualTo(10L);
    assertThat(entity.getSignalId()).isEqualTo(11L);
    assertThat(entity.getUabsEventId()).isEqualTo(12L);
    assertThat(entity.getConsumerId()).isEqualTo(13L);
    assertThat(entity.getAgreementId()).isEqualTo(14L);
    assertThat(entity.getUnauthorizedDebitBalance()).isEqualTo(15L);
    assertThat(entity.getStatus()).isEqualTo("PASS");
    assertThat(entity.getResponseCode()).isEqualTo("200");
    assertThat(entity.getResponseMessage()).isEqualTo("ok");
    assertThat(entity.getAuditRecordDateTime()).isEqualTo(now);
  }

  @Test
  void signalJpaEntity_gettersAndSetters() {
    SignalJpaEntity entity = new SignalJpaEntity();
    entity.setSignalId(1L);
    entity.setAgreementId(2L);
    entity.setSignalStartDate(LocalDate.of(2024, 1, 1));
    entity.setSignalEndDate(LocalDate.of(2024, 1, 5));

    assertThat(entity.getSignalId()).isEqualTo(1L);
    assertThat(entity.getAgreementId()).isEqualTo(2L);
    assertThat(entity.getSignalStartDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    assertThat(entity.getSignalEndDate()).isEqualTo(LocalDate.of(2024, 1, 5));
  }

  @Test
  void accountBalanceJpaEntity_gettersAndSetters() {
    AccountBalanceJpaEntity entity = new AccountBalanceJpaEntity();
    ProductRiskMonitoringJpaEntity prm = new ProductRiskMonitoringJpaEntity();
    prm.setGrv((short) 2);
    prm.setProductId((short) 7);
    prm.setCurrencyCode("EUR");
    prm.setMonitorCW014Signal("Y");
    prm.setMonitorKraandicht("Y");
    prm.setReportCW014ToCEH("Y");
    prm.setReportCW014ToDial("Y");
    entity.setAgreementId(1L);
    entity.setGrv(prm);
    entity.setIban("iban");
    entity.setLifeCycleStatus((short) 1);
    entity.setBcNumber(3L);
    entity.setCurrencyCode("EUR");
    entity.setBookDate(LocalDate.of(2024, 1, 1));
    entity.setUnauthorizedDebitBalance(4L);
    entity.setLastBookDateBalanceCrToDt(LocalDate.of(2023, 12, 31));
    entity.setIsAgreementPartOfAcbs("Y");

    assertThat(entity.getAgreementId()).isEqualTo(1L);
    assertThat(entity.getGrv().getGrv()).isEqualTo((short) 2);
    assertThat(entity.getGrv().getProductId()).isEqualTo((short) 7);
    assertThat(entity.getIban()).isEqualTo("iban");
    assertThat(entity.getLifeCycleStatus()).isEqualTo((short) 1);
    assertThat(entity.getBcNumber()).isEqualTo(3L);
    assertThat(entity.getCurrencyCode()).isEqualTo("EUR");
    assertThat(entity.getBookDate()).isEqualTo(LocalDate.of(2024, 1, 1));
    assertThat(entity.getUnauthorizedDebitBalance()).isEqualTo(4L);
    assertThat(entity.getLastBookDateBalanceCrToDt()).isEqualTo(LocalDate.of(2023, 12, 31));
    assertThat(entity.getIsAgreementPartOfAcbs()).isEqualTo("Y");
  }

  @Test
  void cehResponseInitialEvent_andId_roundTrip() {
    CehResponseInitialEventId id = new CehResponseInitialEventId("abc", 9L);
    CehResponseInitialEvent entity = new CehResponseInitialEvent(id);
    assertThat(entity.getId().getCehInitialEventId()).isEqualTo("abc");
    assertThat(entity.getId().getSignalId()).isEqualTo(9L);
  }
}
