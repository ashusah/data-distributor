package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = DataDistributorApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class OverdueDispatchIntegrationTest {

  @Autowired
  private SignalEventProcessingUseCase processingUseCase;
  @Autowired
  private SignalEventJpaRepository eventRepo;
  @Autowired
  private SignalAuditRepository auditRepo;
  @Autowired
  private SignalJpaRepository signalRepo;
  @Autowired
  private AccountBalanceJpaRepository accountRepo;

  private final LocalDate startDate = LocalDate.of(2025, 12, 1);

  @BeforeEach
  void setup() {
    auditRepo.deleteAll();
    eventRepo.deleteAll();
    signalRepo.deleteAll();
    accountRepo.deleteAll();
  }

  @Test
  void sendsEarliestOverlimitWhenDpdExceedsThresholdEvenWithoutTodayEvent() {
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setSignalId(900L);
    signal.setAgreementId(800L);
    signal.setSignalStartDate(startDate);
    signalRepo.save(signal);

    SignalEventJpaEntity event = new SignalEventJpaEntity();
    event.setSignalId(signal.getSignalId());
    event.setAgreementId(signal.getAgreementId());
    event.setEventRecordDateTime(LocalDateTime.of(startDate, LocalTime.of(8, 0)));
    event.setEventStatus("OVERLIMIT_SIGNAL");
    event.setEventType("OVERLIMIT_SIGNAL");
    event.setUnauthorizedDebitBalance(10L);
    event.setBookDate(startDate);
    createAccount(signal.getAgreementId());
    eventRepo.save(event);

    LocalDate processingDate = startDate.plusDays(5); // DPD6, no events on this date
    processingUseCase.processEventsForDate("job-overdue", processingDate);

    assertThat(auditRepo.findAll()).hasSize(1);
    assertThat(auditRepo.findAll().get(0).getUabsEventId()).isEqualTo(event.getUabsEventId());
  }

  private void createAccount(long agreementId) {
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
    acct.setGrv(prm);
    acct.setIban("DE1234567890123456");
    acct.setLifeCycleStatus((short) 1);
    acct.setBcNumber(agreementId);
    acct.setCurrencyCode("EUR");
    acct.setBookDate(LocalDate.now());
    acct.setUnauthorizedDebitBalance(0L);
    acct.setLastBookDateBalanceCrToDt(LocalDate.now());
    acct.setIsAgreementPartOfAcbs("Y");
    accountRepo.save(acct);
  }
}
