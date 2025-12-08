package com.datadistributor.support;

import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database setup for integration tests so test classes stay focused on behavior.
 */
public class TestSignalDataSeeder {

  private final SignalEventJpaRepository signalRepo;
  private final SignalAuditRepository auditRepo;
  private final AccountBalanceJpaRepository accountRepo;
  private final SignalJpaRepository signalJpaRepo;
  private final EntityManager entityManager;

  public TestSignalDataSeeder(SignalEventJpaRepository signalRepo,
      SignalAuditRepository auditRepo,
      AccountBalanceJpaRepository accountRepo,
      SignalJpaRepository signalJpaRepo,
      EntityManager entityManager) {
    this.signalRepo = signalRepo;
    this.auditRepo = auditRepo;
    this.accountRepo = accountRepo;
    this.signalJpaRepo = signalJpaRepo;
    this.entityManager = entityManager;
  }

  public void resetData() {
    auditRepo.deleteAll();
    signalRepo.deleteAll();
  }

  public List<Long> seedSignalEvents(LocalDate date, int count) {
    List<Long> ids = new ArrayList<>();
    for (long i = 1; i <= count; i++) {
      long agreementId = 3000L + i;
      AccountBalanceJpaEntity account = ensureAccount(agreementId);
      SignalJpaEntity signal = ensureSignal(2000L + i, agreementId, date.minusDays(5));
      SignalEventJpaEntity entity = new SignalEventJpaEntity();
      entity.setSignal(signal);
      entity.setAccountBalance(account);
      entity.setEventRecordDateTime(LocalDateTime.of(date, LocalTime.of(1, 0)));
      entity.setEventType("CONTRACT_UPDATE");
      entity.setEventStatus("OVERLIMIT_SIGNAL");
      entity.setUnauthorizedDebitBalance(300L);
      entity.setBookDate(date.minusDays(5));
      entity.setGrv(ensureProductRiskMonitoring((short) 1));
      entity.setProductId((short) 1);
      SignalEventJpaEntity saved = signalRepo.save(entity);
      ids.add(saved.getUabsEventId());
    }
    return ids;
  }

  private AccountBalanceJpaEntity ensureAccount(long agreementId) {
    if (accountRepo.existsById(agreementId)) {
      return accountRepo.findById(agreementId).orElseThrow();
    }
    AccountBalanceJpaEntity acct = new AccountBalanceJpaEntity();
    acct.setAgreementId(agreementId);
    acct.setCurrencyCode("EUR");
    acct.setBcNumber(agreementId);
    acct.setBookDate(LocalDate.now());
    acct.setGrv(ensureProductRiskMonitoring((short) 1));
    acct.setIban("DE1234567890123456");
    acct.setLifeCycleStatus((short) 1);
    acct.setUnauthorizedDebitBalance(0L);
    acct.setLastBookDateBalanceCrToDt(LocalDate.now());
    acct.setIsAgreementPartOfAcbs("Y");
    accountRepo.save(acct);
    return acct;
  }

  private SignalJpaEntity ensureSignal(long signalId, long agreementId, LocalDate start) {
    // Since signalId is now auto-generated, find by agreementId and startDate
    SignalJpaEntity existing = signalJpaRepo.findAll().stream()
        .filter(s -> s.getAgreementId() != null && s.getAgreementId().equals(agreementId)
            && s.getSignalStartDate() != null && s.getSignalStartDate().equals(start))
        .findFirst()
        .orElse(null);
    
    if (existing != null) {
      return existing;
    }
    
    SignalJpaEntity signal = new SignalJpaEntity();
    // Don't set signalId - it will be auto-generated
    signal.setAgreementId(agreementId);
    signal.setSignalStartDate(start);
    signal.setSignalEndDate(start.plusDays(30));
    signal = signalJpaRepo.save(signal);
    return signal;
  }

  private ProductRiskMonitoringJpaEntity ensureProductRiskMonitoring(short grv) {
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
