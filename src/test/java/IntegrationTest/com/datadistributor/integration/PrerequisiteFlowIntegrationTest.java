package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PrerequisiteFlowIntegrationTest extends AbstractIntegrationTest {

  @Test
  void batchAbortsWhenPriorAuditMissing() {
    long agreementId = 11_000L;
    saveAccount(agreementId, 900_001L);
    SignalEventJpaEntity priorEvent = saveEvent(10_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = priorEvent.getSignal().getSignalId();
    saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-prereq-missing", targetDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(auditRepo.count()).isZero();
  }

  @Test
  void batchProceedsWhenPriorAuditPasses() {
    long agreementId = 21_000L;
    saveAccount(agreementId, 900_002L);
    SignalEventJpaEntity prior = saveEvent(20_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = prior.getSignal().getSignalId();
    saveAuditPass(prior);
    SignalEventJpaEntity current = saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-prereq-pass", targetDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(auditRepo.count()).isEqualTo(2); // prior + current
    assertThat(cehInitRepo.findFirstByIdSignalId(signalId)).isPresent();
    assertThat(auditRepo.findAll())
        .anyMatch(a -> a.getUabsEventId().equals(current.getUabsEventId()) && "PASS".equals(a.getStatus()));
  }

  @Test
  void batchAbortsWhenLatestPriorAuditIsFailAfterRetries() {
    long agreementId = 31_000L;
    saveAccount(agreementId, 900_003L);
    SignalEventJpaEntity prior = saveEvent(30_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = prior.getSignal().getSignalId();
    // multiple audit attempts, last one FAIL -> should block today
    saveAudit(prior, "FAIL", "500");
    saveAudit(prior, "FAIL", "500");

    saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-prereq-multi-fail", targetDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(auditRepo.count()).isEqualTo(2); // only prior attempts recorded
  }

  @Test
  void batchProceedsWhenFinalPriorAuditIsPassAfterFails() {
    long agreementId = 41_000L;
    saveAccount(agreementId, 900_004L);
    SignalEventJpaEntity prior = saveEvent(40_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = prior.getSignal().getSignalId();
    // earlier failures, then a final PASS -> should allow today
    saveAudit(prior, "FAIL", "500");
    saveAudit(prior, "FAIL", "500");
    saveAudit(prior, "PASS", "200");

    SignalEventJpaEntity current = saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-prereq-fail-then-pass", targetDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(auditRepo.findAll())
        .anyMatch(a -> a.getUabsEventId().equals(current.getUabsEventId()) && "PASS".equals(a.getStatus()));
  }

  private void saveAudit(SignalEventJpaEntity event, String status, String code) {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setAuditRecordDateTime(LocalDateTime.now());
    audit.setAgreementId(event.getAccountBalance().getAgreementId());
    audit.setSignalId(event.getSignal().getSignalId());
    audit.setUabsEventId(event.getUabsEventId());
    audit.setConsumerId(1L);
    audit.setUnauthorizedDebitBalance(event.getUnauthorizedDebitBalance());
    audit.setStatus(status);
    audit.setResponseCode(code);
    audit.setResponseMessage(status);
    auditRepo.save(audit);
  }
}
