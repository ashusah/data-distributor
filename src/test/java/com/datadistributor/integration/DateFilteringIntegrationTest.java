package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DateFilteringIntegrationTest extends AbstractIntegrationTest {

  @Test
  void processesOnlyEventsMatchingTargetDate() {
    saveAccount(30_000L, 900_010L);
    var target = saveEvent(30_001L, 30_000L, at(targetDate, 2), "OVERLIMIT_SIGNAL");
    saveEvent(30_002L, 30_000L, at(targetDate.plusDays(1), 3), "OVERLIMIT_SIGNAL"); // next day

    var result = processingUseCase.processEventsForDate("it-date-filter", targetDate);

    assertThat(result.getSuccessCount()).isEqualTo(1);
    assertThat(auditRepo.count()).isEqualTo(1);
    assertThat(auditRepo.findAll().get(0).getUabsEventId()).isEqualTo(target.getUabsEventId());
  }
}
