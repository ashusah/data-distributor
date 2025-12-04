package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "stub.api.response-mode=fail")
class FailureFlowIntegrationTest extends AbstractIntegrationTest {

  @Test
  void auditRecordsFailureWhenExternalApiFails() {
    long signalId = 50_000L;
    long agreementId = 51_000L;
    saveAccount(agreementId, 900_030L);
    saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");

    var result = processingUseCase.processEventsForDate("it-fail", targetDate);

    assertThat(result.getSuccessCount()).isZero();
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(auditRepo.count()).isEqualTo(1);
    assertThat(auditRepo.findAll().get(0).getStatus()).isNotEqualTo("PASS");
  }
}
