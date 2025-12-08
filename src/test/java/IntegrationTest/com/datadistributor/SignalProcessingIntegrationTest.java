package com.datadistributor;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.support.StubExternalApiConfig;
import com.datadistributor.support.TestSignalDataSeeder;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = {DataDistributorApplication.class,
    StubExternalApiConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class SignalProcessingIntegrationTest {

  @Autowired
  private SignalEventJpaRepository signalRepo;

  @Autowired
  private SignalAuditRepository auditRepo;

  @Autowired
  private AccountBalanceJpaRepository accountRepo;

  @Autowired
  private SignalJpaRepository signalJpaRepo;

  @Autowired
  private SignalEventProcessingUseCase processingUseCase;

  @Autowired
  private EntityManager entityManager;

  private TestSignalDataSeeder dataSeeder;
  private LocalDate testDate;
  private List<Long> expectedIds;

  @BeforeEach
  void setup() {
    dataSeeder = new TestSignalDataSeeder(signalRepo, auditRepo, accountRepo, signalJpaRepo, entityManager);
    dataSeeder.resetData();
    testDate = LocalDate.now();
    expectedIds = dataSeeder.seedSignalEvents(testDate, 3);
  }

  @Test
  void asyncEndpointProcessesAllEventsAndPersistsStatus() throws Exception {
    processingUseCase.processEventsForDate("integration-test", testDate);

    // Wait for processing to complete (simple polling)
    int attempts = 0;
    while (attempts < 30) { // up to ~15s
      if (auditRepo.count() == 3) {
        break;
      }
      Thread.sleep(500);
      attempts++;
    }

    // Assertions
    assertThat(auditRepo.count()).isEqualTo(3);
    auditRepo.findAll().forEach(rec -> {
      assertThat(rec.getStatus()).isEqualTo("PASS");
      assertThat(rec.getResponseCode()).isNotNull();
    });

    List<Long> sentIds = auditRepo.findAll()
        .stream()
        .map(rec -> rec.getUabsEventId())
        .sorted()
        .toList();
    assertThat(sentIds).containsExactlyInAnyOrderElementsOf(expectedIds);
  }
}
