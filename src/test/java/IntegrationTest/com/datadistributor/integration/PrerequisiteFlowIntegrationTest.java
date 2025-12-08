package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PrerequisiteFlowIntegrationTest extends AbstractIntegrationTest {

  @Test
  void batchProceedsWhenPriorAuditMissing() {
    // When there's no audit entry for the previous event, it means the event was never sent
    // Processing should continue (new behavior)
    long agreementId = 11_000L;
    saveAccount(agreementId, 900_001L);
    SignalEventJpaEntity priorEvent = saveEvent(10_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = priorEvent.getSignal().getSignalId();
    long priorEventId = priorEvent.getUabsEventId();
    SignalEventJpaEntity current = saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    // Verify no audit exists for prior event before processing
    assertThat(auditRepo.findAll())
        .noneMatch(a -> a.getUabsEventId().equals(priorEventId));

    var result = processingUseCase.processEventsForDate("it-prereq-missing", targetDate);

    // No audit entry means event was never sent, so processing should continue
    assertThat(result.getSuccessCount()).isEqualTo(1);
    // Verify that an audit was created (selector may send earliest OVERLIMIT which is the prior event)
    assertThat(auditRepo.findAll())
        .anyMatch(a -> "PASS".equals(a.getStatus()));
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

  @Test
  void batchAbortsWhenLatestPriorAuditIsFail() {
    // When latest audit entry for previous event is FAIL, batch should be stopped
    long agreementId = 51_000L;
    saveAccount(agreementId, 900_005L);
    SignalEventJpaEntity prior = saveEvent(50_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = prior.getSignal().getSignalId();
    // Multiple audit attempts, latest one is FAIL -> should block today
    saveAudit(prior, "PASS", "200");
    saveAudit(prior, "FAIL", "500"); // Latest audit is FAIL

    saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-prereq-latest-fail", targetDate);

    assertThat(result.getSuccessCount()).isZero();
    // Only prior audit attempts recorded, no new audit for current event
    assertThat(auditRepo.findAll())
        .allMatch(a -> a.getUabsEventId().equals(prior.getUabsEventId()));
  }

  @Test
  void batchProceedsWhenPriorEventHasNoAuditEntry() {
    // When previous event has no audit entry, it means it was never sent
    // Processing should continue (this is the key new behavior)
    long agreementId = 61_000L;
    saveAccount(agreementId, 900_006L);
    SignalEventJpaEntity prior = saveEvent(60_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = prior.getSignal().getSignalId();
    long priorEventId = prior.getUabsEventId();
    // No audit entry for prior event
    
    SignalEventJpaEntity current = saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    // Verify no audit exists for prior event before processing
    assertThat(auditRepo.findAll())
        .noneMatch(a -> a.getUabsEventId().equals(priorEventId));

    var result = processingUseCase.processEventsForDate("it-prereq-no-audit", targetDate);

    // Should proceed because no audit entry means event was never sent
    assertThat(result.getSuccessCount()).isEqualTo(1);
    // Verify that an audit was created (selector may send earliest OVERLIMIT which is the prior event)
    assertThat(auditRepo.findAll())
        .anyMatch(a -> "PASS".equals(a.getStatus()));
  }

  @Test
  void batchProceedsWhenFirstEventForSignal() {
    // When there's no previous event for the signal, processing should continue
    long agreementId = 71_000L;
    saveAccount(agreementId, 900_007L);
    SignalEventJpaEntity firstEvent = saveEvent(70_000L, agreementId, at(targetDate, 1), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-prereq-first-event", targetDate);

    // First event has no prerequisite, should proceed
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(auditRepo.findAll())
        .anyMatch(a -> a.getUabsEventId().equals(firstEvent.getUabsEventId()) && "PASS".equals(a.getStatus()));
  }

  @Test
  void batchUsesLatestAuditEntryWhenMultipleExist() {
    // When multiple audit entries exist for the same event, use the latest one (by timestamp)
    long agreementId = 81_000L;
    saveAccount(agreementId, 900_008L);
    SignalEventJpaEntity prior = saveEvent(80_000L, agreementId, at(targetDate.minusDays(1), 1), "OVERLIMIT_SIGNAL");
    long signalId = prior.getSignal().getSignalId();
    
    // Create multiple audit entries with different timestamps
    // Order matters: latest timestamp should be checked
    saveAuditWithTimestamp(prior, "FAIL", "500", at(targetDate.minusDays(1), 10));
    saveAuditWithTimestamp(prior, "PASS", "200", at(targetDate.minusDays(1), 12)); // Latest - should be used
    
    SignalEventJpaEntity current = saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-prereq-multiple-audits", targetDate);

    // Latest audit is PASS, so should proceed
    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(auditRepo.findAll())
        .anyMatch(a -> a.getUabsEventId().equals(current.getUabsEventId()) && "PASS".equals(a.getStatus()));
  }

  private void saveAudit(SignalEventJpaEntity event, String status, String code) {
    saveAuditWithTimestamp(event, status, code, LocalDateTime.now());
  }

  private void saveAuditWithTimestamp(SignalEventJpaEntity event, String status, String code, LocalDateTime timestamp) {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setAuditRecordDateTime(timestamp);
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
