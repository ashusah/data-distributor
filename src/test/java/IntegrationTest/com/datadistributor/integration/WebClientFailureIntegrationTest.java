package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import com.datadistributor.support.TestSignalDataSeeder;
import com.datadistributor.support.FailingWebClientStubConfig;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {
    DataDistributorApplication.class,
    FailingWebClientStubConfig.class
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "data-distributor.external-api.use-blocking-client=false",
    "data-distributor.external-api.base-url=http://localhost:8081",
    "data-distributor.external-api.retry.attempts=2",
    "data-distributor.external-api.retry.backoff-seconds=0",
    "data-distributor.external-api.retry.max-backoff-seconds=0"
})
class WebClientFailureIntegrationTest {

  @Autowired
  SignalEventProcessingUseCase processingUseCase;
  @Autowired
  SignalAuditRepository auditRepo;
  @Autowired
  SignalEventJpaRepository eventRepo;
  @Autowired
  AccountBalanceJpaRepository accountRepo;
  @Autowired
  SignalJpaRepository signalJpaRepo;
  @Autowired
  AtomicInteger stubCallCount;
  TestSignalDataSeeder seeder;

  @BeforeEach
  void setUp() {
    seeder = new TestSignalDataSeeder(eventRepo, auditRepo, accountRepo, signalJpaRepo);
    seeder.resetData();
    stubCallCount.set(0);
  }

  @Test
  void webClientRetriesAndAuditsOnFailure() {
    seeder.seedSignalEvents(LocalDate.of(2025, 12, 2), 1);
    processingUseCase.processEventsForDate("job-webclient-fail", LocalDate.of(2025, 12, 2));

    // initial call + 2 retries
    assertThat(stubCallCount.get()).isEqualTo(3);
    assertThat(auditRepo.findAll()).hasSize(1);
    var audit = auditRepo.findAll().get(0);
    assertThat(audit.getStatus()).startsWith("FAIL");
  }
}
