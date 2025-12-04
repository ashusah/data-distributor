package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.support.TestSignalDataSeeder;
import com.datadistributor.support.FailingWebClientStubConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
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
    "data-distributor.external-api.retry.attempts=0",
    "resilience4j.circuitbreaker.instances.signalEventApi.sliding-window-type=COUNT_BASED",
    "resilience4j.circuitbreaker.instances.signalEventApi.sliding-window-size=1",
    "resilience4j.circuitbreaker.instances.signalEventApi.minimum-number-of-calls=1",
    "resilience4j.circuitbreaker.instances.signalEventApi.failure-rate-threshold=1",
    "resilience4j.circuitbreaker.instances.signalEventApi.wait-duration-in-open-state=5s",
    "resilience4j.circuitbreaker.instances.signalEventApi.permitted-number-of-calls-in-half-open-state=1"
})
class WebClientCircuitOpenIntegrationTest {

  @Autowired
  SignalEventProcessingUseCase processingUseCase;
  @Autowired
  SignalAuditRepository auditRepo;
  @Autowired
  SignalEventJpaRepository eventRepo;
  @Autowired
  CircuitBreakerRegistry circuitBreakerRegistry;
  @Autowired
  AtomicInteger stubCallCount;
  TestSignalDataSeeder seeder;

  @BeforeEach
  void setUp() {
    seeder = new TestSignalDataSeeder(eventRepo, auditRepo);
    seeder.resetData();
    stubCallCount.set(0);
    circuitBreakerRegistry.circuitBreaker("signalEventApi").reset();
  }

  @Test
  void circuitOpenAuditsBlock() {
    seeder.seedSignalEvents(LocalDate.of(2025, 12, 2), 1);
    circuitBreakerRegistry.circuitBreaker("signalEventApi").transitionToOpenState();

    processingUseCase.processEventsForDate("job-circuit-open", LocalDate.of(2025, 12, 2));

    assertThat(stubCallCount.get()).isEqualTo(0);
    assertThat(auditRepo.findAll()).hasSize(1);
    var audit = auditRepo.findAll().get(0);
    assertThat(audit.getStatus()).contains("BLOCKED_BY_CIRCUIT");
  }
}
