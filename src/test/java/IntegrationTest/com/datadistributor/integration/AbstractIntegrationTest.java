package com.datadistributor.integration;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.CehResponseInitialEventRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import com.datadistributor.support.StubExternalApiConfig;
import jakarta.persistence.EntityManager;
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
  @Autowired
  protected SignalJpaRepository signalJpaRepo;
  @Autowired
  protected EntityManager entityManager;

  protected LocalDate targetDate;

  @BeforeEach
  void baseSetup() {
    auditRepo.deleteAll();
    cehInitRepo.deleteAll();
    signalRepo.deleteAll();
    signalJpaRepo.deleteAll();
    accountRepo.deleteAll();
    targetDate = LocalDate.now();
  }

  protected SignalEventJpaEntity saveEvent(long signalId, long agreementId, LocalDateTime ts, String status) {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    SignalJpaEntity signal = ensureSignal(signalId, agreementId, ts.toLocalDate());
    entity.setSignal(signal);
    ensureAccount(agreementId); // Ensure account exists
    entity.setAgreementId(agreementId);
    entity.setEventRecordDateTime(ts);
    entity.setEventType("CONTRACT_UPDATE");
    entity.setEventStatus(status);
    entity.setUnauthorizedDebitBalance(500L);
    entity.setBookDate(ts.toLocalDate().minusDays(5));
    entity.setGrv(ensureProductRiskMonitoring((short) 1));
    entity.setProductId((short) 1);
    return signalRepo.save(entity);
  }

  protected void saveAuditPass(SignalEventJpaEntity event) {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setAuditRecordDateTime(LocalDateTime.now());
    audit.setAgreementId(event.getAgreementId());
    audit.setSignalId(event.getSignal().getSignalId());
    audit.setUabsEventId(event.getUabsEventId());
    audit.setConsumerId(1L);
    audit.setUnauthorizedDebitBalance(event.getUnauthorizedDebitBalance());
    audit.setStatus("PASS");
    audit.setResponseCode("200");
    audit.setResponseMessage("ok");
    auditRepo.save(audit);
  }

  protected void saveAccount(long agreementId, long bcNumber) {
    if (accountRepo.existsById(agreementId)) {
      return;
    }
    AccountBalanceJpaEntity acct = new AccountBalanceJpaEntity();
    acct.setAgreementId(agreementId);
    acct.setGrv(ensureProductRiskMonitoring((short) 1));
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

  private AccountBalanceJpaEntity ensureAccount(long agreementId) {
    if (accountRepo.existsById(agreementId)) {
      return accountRepo.findById(agreementId).orElseThrow();
    }
    saveAccount(agreementId, agreementId);
    return accountRepo.findById(agreementId).orElseThrow();
  }

  private SignalJpaEntity ensureSignal(long signalId, long agreementId, LocalDate startDate) {
    // First, try to find by signalId if it exists in the database
    // This handles the case where we want to reuse an existing signal (e.g., from a previous event)
    SignalJpaEntity byId = signalJpaRepo.findById(signalId).orElse(null);
    if (byId != null) {
      return byId;
    }
    
    // If signalId not found, look for existing signal by agreementId
    // For tests creating events on the same signal but different dates, we want to reuse the signal
    // So we look for any signal with the same agreementId, regardless of startDate
    SignalJpaEntity existing = signalJpaRepo.findAll().stream()
        .filter(s -> s.getAgreementId() != null && s.getAgreementId().equals(agreementId))
        .findFirst()
        .orElse(null);
    
    if (existing != null) {
      // Refresh to get latest state and avoid optimistic locking issues
      signalJpaRepo.flush();
      return signalJpaRepo.findById(existing.getSignalId()).orElse(existing);
    }
    
    // Create new signal if none exists
    SignalJpaEntity signal = new SignalJpaEntity();
    // Don't set signalId - it will be auto-generated
    signal.setAgreementId(agreementId);
    signal.setSignalStartDate(startDate);
    signal.setSignalEndDate(startDate.plusDays(30));
    signal = signalJpaRepo.saveAndFlush(signal);
    return signal;
  }

  protected ProductRiskMonitoringJpaEntity ensureProductRiskMonitoring(short grv) {
    // Create entity - MERGE cascade will handle it (persist if new, merge if existing)
    ProductRiskMonitoringJpaEntity prm = new ProductRiskMonitoringJpaEntity();
    prm.setGrv(grv);
    prm.setProductId((short) 1);
    prm.setCurrencyCode("EUR");
    prm.setMonitorCW014Signal("Y");
    prm.setMonitorKraandicht("Y");
    prm.setReportCW014ToCEH("Y");
    prm.setReportCW014ToDial("Y");
    return prm;
  }
}
