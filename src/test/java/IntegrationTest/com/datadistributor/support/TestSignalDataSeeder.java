package com.datadistributor.support;

import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
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

  public TestSignalDataSeeder(SignalEventJpaRepository signalRepo,
      SignalAuditRepository auditRepo,
      AccountBalanceJpaRepository accountRepo,
      SignalJpaRepository signalJpaRepo) {
    this.signalRepo = signalRepo;
    this.auditRepo = auditRepo;
    this.accountRepo = accountRepo;
    this.signalJpaRepo = signalJpaRepo;
  }

  public void resetData() {
    auditRepo.deleteAll();
    signalRepo.deleteAll();
  }

  public List<Long> seedSignalEvents(LocalDate date, int count) {
    List<Long> ids = new ArrayList<>();
    for (long i = 1; i <= count; i++) {
      long agreementId = 3000L + i;
      long signalId = 2000L + i;
      ensureAccount(agreementId);
      ensureSignal(signalId, agreementId, date.minusDays(5));
      SignalEventJpaEntity entity = new SignalEventJpaEntity();
      entity.setSignalId(signalId);
      entity.setAgreementId(agreementId);
      entity.setEventRecordDateTime(LocalDateTime.of(date, LocalTime.of(1, 0)));
      entity.setEventType("CONTRACT_UPDATE");
      entity.setEventStatus("OVERLIMIT_SIGNAL");
      entity.setUnauthorizedDebitBalance(300L);
      entity.setBookDate(date.minusDays(5));
      entity.setGrv((short) 1);
      entity.setProductId((short) 1);
      SignalEventJpaEntity saved = signalRepo.save(entity);
      ids.add(saved.getUabsEventId());
    }
    return ids;
  }

  private void ensureAccount(long agreementId) {
    if (accountRepo.existsById(agreementId)) {
      return;
    }
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
    acct.setCurrencyCode("EUR");
    acct.setBcNumber(agreementId);
    acct.setBookDate(LocalDate.now());
    acct.setGrv(prm);
    acct.setIban("DE1234567890123456");
    acct.setLifeCycleStatus((short) 1);
    acct.setUnauthorizedDebitBalance(0L);
    acct.setLastBookDateBalanceCrToDt(LocalDate.now());
    acct.setIsAgreementPartOfAcbs("Y");
    accountRepo.save(acct);
  }

  private void ensureSignal(long signalId, long agreementId, LocalDate start) {
    if (signalJpaRepo.existsById(signalId)) {
      return;
    }
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setSignalId(signalId);
    signal.setAgreementId(agreementId);
    signal.setSignalStartDate(start);
    signal.setSignalEndDate(start.plusDays(30));
    signalJpaRepo.save(signal);
  }
}
