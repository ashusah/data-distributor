package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.outadapter.entity.AccountBalanceOverviewJpaEntity;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceOverviewJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.CehResponseInitialEventRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.support.StubExternalApiConfig;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(
    classes = {DataDistributorApplication.class, StubExternalApiConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SignalEventProcessingIntegrationTests {

  @Autowired
  private SignalEventProcessingUseCase processingUseCase;
  @Autowired
  private SignalEventJpaRepository signalRepo;
  @Autowired
  private SignalAuditRepository auditRepo;
  @Autowired
  private CehResponseInitialEventRepository cehInitRepo;
  @Autowired
  private AccountBalanceOverviewJpaRepository accountRepo;

  private LocalDate targetDate;

  @BeforeEach
  void setup() {
    auditRepo.deleteAll();
    cehInitRepo.deleteAll();
    signalRepo.deleteAll();
    accountRepo.deleteAll();
    targetDate = LocalDate.now();
  }

  @Test
  void processesEventsAndStoresAuditAndInitialCeh() {
    SeedResult seed = seedSignalWithPriorAudit(targetDate.minusDays(1));
    seedAccount(seed.agreementId(), 999_111L);

    processingUseCase.processEventsForDate("it-success", targetDate);

    List<SignalAuditJpaEntity> audits = auditRepo.findAll();
    assertThat(audits).hasSize(2); // prior + current
    SignalAuditJpaEntity audit = audits.stream()
        .filter(a -> a.getUabsEventId().equals(seed.currentEventUabsId()))
        .findFirst()
        .orElseThrow();
    assertThat(audit.getStatus()).isEqualTo("PASS");
    assertThat(audit.getResponseCode()).isNotBlank();
    assertThat(cehInitRepo.findFirstByIdSignalId(seed.signalId())).isPresent();
  }

  @Test
  void abortsBatchWhenPriorEventNotSuccessful() {
    SeedResult seed = seedSignalWithoutPriorAudit(targetDate.minusDays(1));
    seedAccount(seed.agreementId(), 888_222L);

    var result = processingUseCase.processEventsForDate("it-prereq-fail", targetDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(auditRepo.count()).isZero();
  }

  private SeedResult seedSignalWithPriorAudit(LocalDate priorDate) {
    // prior event with PASS audit
    SignalEventJpaEntity prior = new SignalEventJpaEntity();
    prior.setSignalId(5000L);
    prior.setAgreementId(6000L);
    prior.setEventRecordDateTime(LocalDateTime.of(priorDate, LocalTime.of(1, 0)));
    prior.setEventType("CONTRACT_UPDATE");
    prior.setEventStatus("OVERLIMIT_SIGNAL");
    prior.setUnauthorizedDebitBalance(500L);
    prior.setBookDate(priorDate.minusDays(5));
    prior.setGrv((short) 1);
    prior.setProductId((short) 1);
    SignalEventJpaEntity savedPrior = signalRepo.save(prior);

    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setAuditRecordDateTime(LocalDateTime.now());
    audit.setAgreementId(savedPrior.getAgreementId());
    audit.setSignalId(savedPrior.getSignalId());
    audit.setUabsEventId(savedPrior.getUabsEventId());
    audit.setConsumerId(1L);
    audit.setUnauthorizedDebitBalance(savedPrior.getUnauthorizedDebitBalance());
    audit.setStatus("PASS");
    audit.setResponseCode("200");
    audit.setResponseMessage("ok");
    auditRepo.save(audit);

    // current day event to process
    SignalEventJpaEntity current = new SignalEventJpaEntity();
    current.setSignalId(savedPrior.getSignalId());
    current.setAgreementId(savedPrior.getAgreementId());
    current.setEventRecordDateTime(LocalDateTime.of(targetDate, LocalTime.of(2, 0)));
    current.setEventType("CONTRACT_UPDATE");
    current.setEventStatus("OVERLIMIT_SIGNAL");
    current.setUnauthorizedDebitBalance(500L);
    current.setBookDate(targetDate.minusDays(5));
    current.setGrv((short) 1);
    current.setProductId((short) 1);
    SignalEventJpaEntity savedCurrent = signalRepo.save(current);

    return new SeedResult(savedPrior.getSignalId(), savedCurrent.getAgreementId(), savedCurrent.getUabsEventId());
  }

  private SeedResult seedSignalWithoutPriorAudit(LocalDate priorDate) {
    SignalEventJpaEntity prior = new SignalEventJpaEntity();
    prior.setSignalId(7000L);
    prior.setAgreementId(8000L);
    prior.setEventRecordDateTime(LocalDateTime.of(priorDate, LocalTime.of(1, 0)));
    prior.setEventType("CONTRACT_UPDATE");
    prior.setEventStatus("OVERLIMIT_SIGNAL");
    prior.setUnauthorizedDebitBalance(500L);
    prior.setBookDate(priorDate.minusDays(5));
    prior.setGrv((short) 1);
    prior.setProductId((short) 1);
    signalRepo.save(prior);

    SignalEventJpaEntity current = new SignalEventJpaEntity();
    current.setSignalId(prior.getSignalId());
    current.setAgreementId(prior.getAgreementId());
    current.setEventRecordDateTime(LocalDateTime.of(targetDate, LocalTime.of(2, 0)));
    current.setEventType("CONTRACT_UPDATE");
    current.setEventStatus("OVERLIMIT_SIGNAL");
    current.setUnauthorizedDebitBalance(500L);
    current.setBookDate(targetDate.minusDays(5));
    current.setGrv((short) 1);
    current.setProductId((short) 1);
    SignalEventJpaEntity savedCurrent = signalRepo.save(current);

    return new SeedResult(prior.getSignalId(), savedCurrent.getAgreementId(), savedCurrent.getUabsEventId());
  }

  private void seedAccount(long agreementId, long bcNumber) {
    AccountBalanceOverviewJpaEntity acct = new AccountBalanceOverviewJpaEntity();
    acct.setAgreementId(agreementId);
    acct.setGrv((short) 1);
    acct.setIban("DE1234567890123456");
    acct.setLifeCycleStatus((byte) 1);
    acct.setBcNumber(bcNumber);
    acct.setCurrencyCode("EUR");
    acct.setBookDate(LocalDate.now().minusDays(5));
    acct.setUnauthorizedDebitBalance(500L);
    acct.setLastBookDateBalanceCrToDt(LocalDate.now().minusDays(5));
    acct.setIsAgreementPartOfAcbs("Y");
    acct.setIsMarginAccountLinked("N");
    accountRepo.save(acct);
  }

  private record SeedResult(long signalId, long agreementId, long currentEventUabsId) {
  }
}
