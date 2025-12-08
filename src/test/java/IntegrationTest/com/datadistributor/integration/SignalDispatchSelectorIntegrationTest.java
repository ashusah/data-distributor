package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
  @Autowired
  private AccountBalanceJpaRepository accountRepo;

  private final LocalDate start = LocalDate.of(2025, 1, 1);

  @Test
  void breachThenFollowUpAfterInitialSent() {
    long agreementId = 10L;
    SignalJpaEntity signal = saveSignal(1L, agreementId, start, null);
    long signalId = signal.getSignalId();
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
    long agreementId = 20L;
    SignalJpaEntity signal = saveSignal(2L, agreementId, start, start.plusDays(2));
    long signalId = signal.getSignalId();
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
    long agreementId = 30L;
    SignalJpaEntity signal = saveSignal(3L, agreementId, start, start.plusDays(2));
    long signalId = signal.getSignalId();
    saveEventWithBalance(signalId, agreementId, at(start, 8), "OVERLIMIT_SIGNAL", 10L);
    saveEventWithBalance(signalId, agreementId, at(start.plusDays(1), 8), "FINANCIAL_UPDATE", 249L);
    saveEventWithBalance(signalId, agreementId, at(start.plusDays(2), 8), "OUT_OF_OVERLIMIT", 0L);

    assertThat(selector.selectEventsToSend(start)).isEmpty();
    assertThat(selector.selectEventsToSend(start.plusDays(1))).isEmpty();
    assertThat(selector.selectEventsToSend(start.plusDays(2))).isEmpty();
  }

  private SignalJpaEntity saveSignal(long signalId, long agreementId, LocalDate startDate, LocalDate endDate) {
    // Since signalId is now auto-generated, we need to find existing signal or create new one
    // For tests, we'll try to find by agreementId and startDate, or create new
    SignalJpaEntity signal = signalRepo.findAll().stream()
        .filter(s -> s.getAgreementId() != null && s.getAgreementId().equals(agreementId)
            && s.getSignalStartDate() != null && s.getSignalStartDate().equals(startDate))
        .findFirst()
        .orElse(null);
    
    if (signal == null) {
      signal = new SignalJpaEntity();
      // Don't set signalId - it will be auto-generated
      signal.setAgreementId(agreementId);
      signal.setSignalStartDate(startDate);
      signal.setSignalEndDate(endDate);
      signal = signalRepo.save(signal);
    } else {
      // Update end date if provided
      if (endDate != null) {
        signal.setSignalEndDate(endDate);
        signal = signalRepo.save(signal);
      }
    }
    return signal;
  }

  private SignalEventJpaEntity saveEventWithBalance(long signalId,
                                                    long agreementId,
                                                    LocalDateTime ts,
                                                    String status,
                                                    long balance) {
    SignalJpaEntity signal = signalRepo.findById(signalId)
        .orElseGet(() -> {
          SignalJpaEntity newSignal = new SignalJpaEntity();
          newSignal.setAgreementId(agreementId);
          newSignal.setSignalStartDate(ts.toLocalDate());
          return signalRepo.save(newSignal);
        });
    saveAccount(agreementId, agreementId);
    AccountBalanceJpaEntity account = accountRepo.findById(agreementId).orElseThrow();
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setSignal(signal);
    entity.setAccountBalance(account);
    entity.setEventRecordDateTime(ts);
    entity.setEventStatus(status);
    entity.setEventType(status);
    entity.setUnauthorizedDebitBalance(balance);
    entity.setBookDate(ts.toLocalDate());
    entity.setGrv(ensureProductRiskMonitoring((short) 1));
    entity.setProductId((short) 1);
    return eventRepo.save(entity);
  }
}
