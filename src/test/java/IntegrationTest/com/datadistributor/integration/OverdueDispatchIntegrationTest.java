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
import jakarta.persistence.EntityManager;
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
  @Autowired
  private EntityManager entityManager;

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
    // Don't set signalId - it will be auto-generated
    signal.setAgreementId(800L);
    signal.setSignalStartDate(startDate);
    signal = signalRepo.save(signal);

    createAccount(signal.getAgreementId());
    SignalEventJpaEntity event = new SignalEventJpaEntity();
    event.setSignal(signal);
    event.setAgreementId(signal.getAgreementId());
    event.setEventRecordDateTime(LocalDateTime.of(startDate, LocalTime.of(8, 0)));
    event.setEventStatus("OVERLIMIT_SIGNAL");
    event.setEventType("OVERLIMIT_SIGNAL");
    event.setUnauthorizedDebitBalance(10L);
    event.setBookDate(startDate);
    event.setGrv(ensureProductRiskMonitoring((short) 1));
    event.setProductId((short) 1);
    eventRepo.save(event);

    LocalDate processingDate = startDate.plusDays(5); // DPD6, no events on this date
    processingUseCase.processEventsForDate("job-overdue", processingDate);

    assertThat(auditRepo.findAll()).hasSize(1);
    assertThat(auditRepo.findAll().get(0).getUabsEventId()).isEqualTo(event.getUabsEventId());
  }

  private AccountBalanceJpaEntity createAccount(long agreementId) {
    if (accountRepo.existsById(agreementId)) {
      return accountRepo.findById(agreementId).orElseThrow();
    }
    AccountBalanceJpaEntity acct = new AccountBalanceJpaEntity();
    acct.setAgreementId(agreementId);
    acct.setGrv(ensureProductRiskMonitoring((short) 1));
    acct.setIban("DE1234567890123456");
    acct.setLifeCycleStatus((short) 1);
    acct.setBcNumber(agreementId);
    acct.setCurrencyCode("EUR");
    acct.setBookDate(LocalDate.now());
    acct.setUnauthorizedDebitBalance(0L);
    acct.setLastBookDateBalanceCrToDt(LocalDate.now());
    acct.setIsAgreementPartOfAcbs("Y");
    accountRepo.save(acct);
    return acct;
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
