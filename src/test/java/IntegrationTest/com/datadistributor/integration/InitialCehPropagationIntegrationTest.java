package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.outadapter.entity.CehResponseInitialEventEntity;
import com.datadistributor.outadapter.entity.CehResponseInitialEventId;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class InitialCehPropagationIntegrationTest extends AbstractIntegrationTest {

  @Test
  void initialCehIdStoredOnFirstOverlimitAndReused() {
    long agreementId = 41_000L;
    saveAccount(agreementId, 900_020L);

    var firstDay = targetDate.minusDays(1);
    SignalEventJpaEntity firstEvent = saveEvent(40_000L, agreementId, at(firstDay, 2), "OVERLIMIT_SIGNAL");
    long signalId = firstEvent.getSignal().getSignalId();
    processingUseCase.processEventsForDate("it-init-ceh-day1", firstDay);
    String cehId = cehInitRepo.findFirstByIdSignalId(signalId)
        .map(CehResponseInitialEventEntity::getId)
        .map(CehResponseInitialEventId::getCehInitialEventId)
        .orElseThrow();

    saveEvent(signalId, agreementId, at(targetDate, 2), "OVERLIMIT_SIGNAL");
    processingUseCase.processEventsForDate("it-init-ceh-day2", targetDate);

    assertThat(cehInitRepo.findFirstByIdSignalId(signalId))
        .isPresent()
        .get()
        .extracting(c -> c.getId().getCehInitialEventId())
        .isEqualTo(cehId);
  }
}
