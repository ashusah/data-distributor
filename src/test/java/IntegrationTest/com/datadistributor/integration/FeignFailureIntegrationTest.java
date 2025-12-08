package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import com.datadistributor.support.TestSignalDataSeeder;
import com.datadistributor.outadapter.web.SignalEventFeignClient;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {
    DataDistributorApplication.class
}, webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "data-distributor.external-api.use-blocking-client=true",
    "data-distributor.external-api.base-url=http://localhost:9999"
})
class FeignFailureIntegrationTest {

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
  @MockBean
  SignalEventFeignClient feignClient;
  @Autowired
  EntityManager entityManager;
  TestSignalDataSeeder seeder;

  @BeforeEach
  void setUp() {
    seeder = new TestSignalDataSeeder(eventRepo, auditRepo, accountRepo, signalJpaRepo);
    seeder.resetData();
    Mockito.when(feignClient.postSignalEvent(Mockito.any())).thenThrow(new IllegalStateException("Feign down"));
  }

  @Test
  void feignBlockingFailurePersistsAudit() {
    seeder.seedSignalEvents(LocalDate.of(2025, 12, 2), 1);
    processingUseCase.processEventsForDate("job-feign-fail", LocalDate.of(2025, 12, 2));

    assertThat(auditRepo.findAll()).hasSize(1);
    var audit = auditRepo.findAll().get(0);
    assertThat(audit.getStatus()).startsWith("FAIL");
  }
}
