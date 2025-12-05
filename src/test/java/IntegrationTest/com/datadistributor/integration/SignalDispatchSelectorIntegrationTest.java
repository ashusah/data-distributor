package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration-level selector checks for the key DPD scenarios without triggering the
 * prerequisite audit guard (we invoke the selector directly).
 */
class SignalDispatchSelectorIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private SignalDispatchSelectorUseCase selector;
  @Autowired
  private SignalJpaRepository signalRepo;
  @Autowired
  private SignalEventJpaRepository eventRepo;

  private final LocalDate start = LocalDate.of(2025, 1, 1);

  @Test
  void breachThenFollowUpAfterInitialSent() {
    long signalId = 1L;
    long agreementId = 10L;
    saveSignal(signalId, agreementId, start, null);
    SignalEventJpaEntity dpd1 = saveEventWithBalance(signalId, agreementId, at(start, 8), "OVERLIMIT_SIGNAL", 10L);
    saveEventWithBalance(signalId, agreementId, at(start.plusDays(1), 8), "FINANCIAL_UPDATE", 250L);
    SignalEventJpaEntity dpd3 = saveEventWithBalance(signalId, agreementId, at(start.plusDays(2), 8), "FINANCIAL_UPDATE", 20L);

    assertThat(selector.selectEventsToSend(start)).isEmpty();
    var day2 = selector.selectEventsToSend(start.plusDays(1));
    assertThat(day2)
        .extracting(SignalEvent::getUabsEventId)
        .containsExactly(dpd1.getUabsEventId());
    saveAuditPass(dpd1);
    var day3 = selector.selectEventsToSend(start.plusDays(2));
    assertThat(day3)
        .extracting(SignalEvent::getUabsEventId)
        .containsExactly(dpd3.getUabsEventId());
  }

  @Test
  void breachThenClosureSendsClosure() {
    long signalId = 2L;
    long agreementId = 20L;
    saveSignal(signalId, agreementId, start, start.plusDays(2));
    SignalEventJpaEntity dpd1 = saveEventWithBalance(signalId, agreementId, at(start, 8), "OVERLIMIT_SIGNAL", 10L);
    saveEventWithBalance(signalId, agreementId, at(start.plusDays(1), 8), "FINANCIAL_UPDATE", 250L);
    SignalEventJpaEntity closure = saveEventWithBalance(signalId, agreementId, at(start.plusDays(2), 8), "OUT_OF_OVERLIMIT", 0L);

    assertThat(selector.selectEventsToSend(start)).isEmpty();
    var day2 = selector.selectEventsToSend(start.plusDays(1));
    assertThat(day2)
        .extracting(SignalEvent::getUabsEventId)
        .containsExactly(dpd1.getUabsEventId());
    saveAuditPass(dpd1);
    assertThat(selector.selectEventsToSend(start.plusDays(2)))
        .extracting(SignalEvent::getUabsEventId)
        .containsExactly(closure.getUabsEventId());
  }

  @Test
  void noBreachThenClosureSendsNothing() {
    long signalId = 3L;
    long agreementId = 30L;
    saveSignal(signalId, agreementId, start, start.plusDays(2));
    saveEventWithBalance(signalId, agreementId, at(start, 8), "OVERLIMIT_SIGNAL", 10L);
    saveEventWithBalance(signalId, agreementId, at(start.plusDays(1), 8), "FINANCIAL_UPDATE", 249L);
    saveEventWithBalance(signalId, agreementId, at(start.plusDays(2), 8), "OUT_OF_OVERLIMIT", 0L);

    assertThat(selector.selectEventsToSend(start)).isEmpty();
    assertThat(selector.selectEventsToSend(start.plusDays(1))).isEmpty();
    assertThat(selector.selectEventsToSend(start.plusDays(2))).isEmpty();
  }

  private SignalJpaEntity saveSignal(long signalId, long agreementId, LocalDate startDate, LocalDate endDate) {
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setSignalId(signalId);
    signal.setAgreementId(agreementId);
    signal.setSignalStartDate(startDate);
    signal.setSignalEndDate(endDate);
    return signalRepo.save(signal);
  }

  private SignalEventJpaEntity saveEventWithBalance(long signalId,
                                                    long agreementId,
                                                    LocalDateTime ts,
                                                    String status,
                                                    long balance) {
    saveAccount(agreementId, agreementId);
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setSignalId(signalId);
    entity.setAgreementId(agreementId);
    entity.setEventRecordDateTime(ts);
    entity.setEventStatus(status);
    entity.setEventType(status);
    entity.setUnauthorizedDebitBalance(balance);
    entity.setBookDate(ts.toLocalDate());
    entity.setGrv((short) 1);
    entity.setProductId((short) 1);
    return eventRepo.save(entity);
  }
}
