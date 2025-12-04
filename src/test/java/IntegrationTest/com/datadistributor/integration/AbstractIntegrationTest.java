package com.datadistributor.integration;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.CehResponseInitialEventRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.support.StubExternalApiConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = {DataDistributorApplication.class, StubExternalApiConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
abstract class AbstractIntegrationTest {

  @Autowired
  protected SignalEventProcessingUseCase processingUseCase;
  @Autowired
  protected SignalEventJpaRepository signalRepo;
  @Autowired
  protected SignalAuditRepository auditRepo;
  @Autowired
  protected CehResponseInitialEventRepository cehInitRepo;
  @Autowired
  protected AccountBalanceJpaRepository accountRepo;

  protected LocalDate targetDate;

  @BeforeEach
  void baseSetup() {
    auditRepo.deleteAll();
    cehInitRepo.deleteAll();
    signalRepo.deleteAll();
    accountRepo.deleteAll();
    targetDate = LocalDate.now();
  }

  protected SignalEventJpaEntity saveEvent(long signalId, long agreementId, LocalDateTime ts, String status) {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setSignalId(signalId);
    entity.setAgreementId(agreementId);
    entity.setEventRecordDateTime(ts);
    entity.setEventType("CONTRACT_UPDATE");
    entity.setEventStatus(status);
    entity.setUnauthorizedDebitBalance(500L);
    entity.setBookDate(ts.toLocalDate().minusDays(5));
    entity.setGrv((short) 1);
    entity.setProductId((short) 1);
    return signalRepo.save(entity);
  }

  protected void saveAuditPass(SignalEventJpaEntity event) {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setAuditRecordDateTime(LocalDateTime.now());
    audit.setAgreementId(event.getAgreementId());
    audit.setSignalId(event.getSignalId());
    audit.setUabsEventId(event.getUabsEventId());
    audit.setConsumerId(1L);
    audit.setUnauthorizedDebitBalance(event.getUnauthorizedDebitBalance());
    audit.setStatus("PASS");
    audit.setResponseCode("200");
    audit.setResponseMessage("ok");
    auditRepo.save(audit);
  }

  protected void saveAccount(long agreementId, long bcNumber) {
    AccountBalanceJpaEntity acct = new AccountBalanceJpaEntity();
    ProductRiskMonitoringJpaEntity prm = new ProductRiskMonitoringJpaEntity();
    prm.setGrv((short) 1);
    prm.setProductId((short) 1);
    prm.setCurrencyCode("EUR");
    prm.setMonitorCW014Signal("Y");
    prm.setMonitorKraandicht("Y");
    prm.setReportCW014ToCEH("Y");
    prm.setReportCW014ToDial("Y");
    acct.setAgreementId(agreementId);
    acct.setGrv(prm);
    acct.setIban("DE1234567890123456");
    acct.setLifeCycleStatus((short) 1);
    acct.setBcNumber(bcNumber);
    acct.setCurrencyCode("EUR");
    acct.setBookDate(LocalDate.now().minusDays(5));
    acct.setUnauthorizedDebitBalance(500L);
    acct.setLastBookDateBalanceCrToDt(LocalDate.now().minusDays(5));
    acct.setIsAgreementPartOfAcbs("Y");
    accountRepo.save(acct);
  }

  protected LocalDateTime at(LocalDate date, int hour) {
    return LocalDateTime.of(date, LocalTime.of(hour, 0));
  }
}
