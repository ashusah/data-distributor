package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventPort;
import com.datadistributor.domain.outport.SignalPort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SignalDispatchDomainSelectorTest {

  @Mock private SignalEventPort signalEventPort;
  @Mock private SignalPort signalPort;
  @Mock private SignalAuditQueryPort auditQueryPort;
  @Mock private InitialCehMappingPort initialCehMappingPort;

  private SignalDispatchDomainSelector selector;
  private Signal signal;
  private final LocalDate startDate = LocalDate.of(2025, 1, 1);
  private final long signalId = 1L;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    selector = new SignalDispatchDomainSelector(
        signalEventPort, signalPort, auditQueryPort, initialCehMappingPort,
        250L, 5L, 1L);
    signal = new Signal();
    signal.setSignalId(signalId);
    signal.setSignalStartDate(startDate);
    signal.setSignalEndDate(startDate.plusDays(30));
    when(signalPort.findBySignalId(signalId)).thenReturn(Optional.of(signal));
    when(signalPort.findByStartDateBefore(any())).thenAnswer(invocation -> {
      LocalDate cutoff = invocation.getArgument(0);
      if (cutoff == null || signal == null || signal.getSignalStartDate() == null) {
        return List.of();
      }
      if (!signal.getSignalStartDate().isAfter(cutoff)) {
        return List.of(signal);
      }
      return List.of();
    });
  }

  @Test
  void lowBalanceNoEvents_untilOverdueOpenEventSentOnDPD6() {
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent lowFU = buildEvent(2L, startDate.plusDays(1), 5, "FINANCIAL_UPDATE");

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(1), List.of(lowFU)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    for (int offset = 0; offset < 6; offset++) {
      LocalDate target = startDate.plusDays(offset);
      List<SignalEvent> selection = selector.selectEventsToSend(target);
      if (offset < 5) {
        assertThat(selection).isEmpty();
      } else {
        assertThat(selection).containsExactly(openEvent);
        openSent.set(true);
      }
    }
  }

  @Test
  void lowBalanceProductSwap_thenOverdueSendsOpenEventOnDPD6() {
    SignalEvent openEvent = buildEvent(11L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent swap = buildEvent(12L, startDate.plusDays(1), 50, "PRODUCT_SWAP");

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(1), List.of(swap)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    for (int offset = 0; offset < 6; offset++) {
      LocalDate target = startDate.plusDays(offset);
      List<SignalEvent> selection = selector.selectEventsToSend(target);
      if (offset < 5) {
        assertThat(selection).isEmpty();
      } else {
        assertThat(selection).containsExactly(openEvent);
        openSent.set(true);
      }
    }
  }

  @Test
  void closedBeforeOverdue_noSignalsDispatched() {
    signal.setSignalEndDate(startDate.plusDays(4));
    SignalEvent openEvent = buildEvent(21L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent swap = buildEvent(22L, startDate.plusDays(1), 50, "PRODUCT_SWAP");
    SignalEvent close = buildEvent(23L, startDate.plusDays(4), 0, "OUT_OF_OVERLIMIT");

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(1), List.of(swap),
        startDate.plusDays(4), List.of(close)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    for (int offset = 0; offset < 6; offset++) {
      LocalDate target = startDate.plusDays(offset);
      List<SignalEvent> selection = selector.selectEventsToSend(target);
      assertThat(selection).isEmpty();
    }
  }

  @Test
  void breachSendsOpenEventThenFutureEventsAfterThreshold() {
    SignalEvent openEvent = buildEvent(31L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent breach = buildEvent(32L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");
    SignalEvent laterBreach = buildEvent(33L, startDate.plusDays(3), 251, "FINANCIAL_UPDATE");
    SignalEvent future = buildEvent(34L, startDate.plusDays(5), 20, "FINANCIAL_UPDATE");

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(1), List.of(breach),
        startDate.plusDays(3), List.of(laterBreach),
        startDate.plusDays(5), List.of(future)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    // DPD1
    assertThat(selector.selectEventsToSend(startDate)).isEmpty();
    // DPD2 - breach day, only open event
    assertThat(selector.selectEventsToSend(startDate.plusDays(1))).containsExactly(openEvent);
    openSent.set(true);
    // DPD3
    assertThat(selector.selectEventsToSend(startDate.plusDays(2))).isEmpty();
    // DPD4 - 251 event
    assertThat(selector.selectEventsToSend(startDate.plusDays(3))).containsExactly(laterBreach);
    // DPD5
    assertThat(selector.selectEventsToSend(startDate.plusDays(4))).isEmpty();
    // DPD6 - 20 event
    assertThat(selector.selectEventsToSend(startDate.plusDays(5))).containsExactly(future);
  }

  @Test
  void breachThenBelowThresholdStillEmitsEvents() {
    SignalEvent openEvent = buildEvent(41L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent breach = buildEvent(42L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");
    SignalEvent small = buildEvent(43L, startDate.plusDays(3), 249, "FINANCIAL_UPDATE");
    SignalEvent future = buildEvent(44L, startDate.plusDays(5), 20, "FINANCIAL_UPDATE");

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(1), List.of(breach),
        startDate.plusDays(3), List.of(small),
        startDate.plusDays(5), List.of(future)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    assertThat(selector.selectEventsToSend(startDate)).isEmpty();
    assertThat(selector.selectEventsToSend(startDate.plusDays(1))).containsExactly(openEvent);
    openSent.set(true);
    assertThat(selector.selectEventsToSend(startDate.plusDays(3))).containsExactly(small);
    assertThat(selector.selectEventsToSend(startDate.plusDays(5))).containsExactly(future);
  }

  @Test
  void overdueThenThresholdOnDPD7_sendsOpenOnDPD6_thenEventOnDPD7() {
    SignalEvent openEvent = buildEvent(51L, startDate, 10, "OVERLIMIT_SIGNAL");
    SignalEvent lateBreach = buildEvent(52L, startDate.plusDays(6), 250, "FINANCIAL_UPDATE");

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(5), List.of(),
        startDate.plusDays(6), List.of(lateBreach)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    assertThat(selector.selectEventsToSend(startDate.plusDays(4))).isEmpty();
    assertThat(selector.selectEventsToSend(startDate.plusDays(5))).containsExactly(openEvent);
    openSent.set(true);
    assertThat(selector.selectEventsToSend(startDate.plusDays(6))).containsExactly(lateBreach);
  }

  @Test
  void breachAndClose_sendsOpenDayThenClosure() {
    SignalEvent openEvent = buildEvent(61L, startDate, 10, "OVERLIMIT_SIGNAL");
    SignalEvent breach = buildEvent(62L, startDate.plusDays(2), 250, "FINANCIAL_UPDATE");
    SignalEvent close = buildEvent(63L, startDate.plusDays(4), 0, "OUT_OF_OVERLIMIT");
    signal.setSignalEndDate(startDate.plusDays(4));

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(2), List.of(breach),
        startDate.plusDays(4), List.of(close)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    assertThat(selector.selectEventsToSend(startDate.plusDays(2))).containsExactly(openEvent);
    openSent.set(true);
    assertThat(selector.selectEventsToSend(startDate.plusDays(3))).isEmpty();
    assertThat(selector.selectEventsToSend(startDate.plusDays(4))).containsExactly(close);
  }

  private void stubEvents(Map<LocalDate, List<SignalEvent>> eventsByDate) {
    when(signalEventPort.getAllSignalEventsOfThisDate(any())).thenAnswer(invocation -> {
      LocalDate day = invocation.getArgument(0);
      return eventsByDate.getOrDefault(day, List.of());
    });
  }

  private void stubInitialState(AtomicBoolean openSent, Long openEventId) {
    when(initialCehMappingPort.findInitialCehId(signalId)).thenAnswer(invocation ->
        openSent.get() ? Optional.of("ceh-" + openEventId) : Optional.empty());
    when(auditQueryPort.isEventSuccessful(eq(openEventId), anyLong())).thenAnswer(invocation -> openSent.get());
    when(auditQueryPort.isEventSuccessful(anyLong(), anyLong())).thenReturn(false);
  }

  private SignalEvent buildEvent(long id, LocalDate date, long balance, String status) {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(id);
    event.setSignalId(signalId);
    event.setEventRecordDateTime(date.atTime(LocalTime.of(10, 0)));
    event.setUnauthorizedDebitBalance(balance);
    event.setEventStatus(status);
    event.setEventType(status);
    event.setBookDate(date);
    return event;
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void selectEventsToSend_handlesNullSignalIdInEvents() {
    SignalEvent eventWithNullSignalId = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    eventWithNullSignalId.setSignalId(null);

    stubEvents(Map.of(startDate, List.of(eventWithNullSignalId)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void selectEventsToSend_handlesEmptyEventsList() {
    stubEvents(Map.of(startDate, List.of()));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateSignal_returnsEarlyWhenSignalNotFound() {
    when(signalPort.findBySignalId(signalId)).thenReturn(Optional.empty());
    SignalEvent event = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate, List.of(event)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateSignal_returnsEarlyWhenSignalStartDateIsNull() {
    signal.setSignalStartDate(null);
    when(signalPort.findBySignalId(signalId)).thenReturn(Optional.of(signal));
    SignalEvent event = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate, List.of(event)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateSignal_returnsEarlyWhenSignalStartDateAfterTarget() {
    signal.setSignalStartDate(startDate.plusDays(1));
    when(signalPort.findBySignalId(signalId)).thenReturn(Optional.of(signal));
    SignalEvent event = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate, List.of(event)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateSignal_returnsEarlyWhenNoEarliestOverlimitEvent() {
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.empty());
    SignalEvent event = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate, List.of(event)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateSignal_skipsWhenClosedWithClosureEventAndNoBreach() {
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent closureEvent = buildEvent(2L, startDate.plusDays(1), 0, "OUT_OF_OVERLIMIT");
    signal.setSignalEndDate(null);

    stubEvents(Map.of(startDate.plusDays(1), List.of(closureEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateSignal_skipsWhenClosedWithoutBreachAndWithinThreshold() {
    signal.setSignalEndDate(startDate.plusDays(2));
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent lowEvent = buildEvent(2L, startDate.plusDays(1), 100, "FINANCIAL_UPDATE");

    stubEvents(Map.of(startDate.plusDays(1), List.of(lowEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateSignal_sendsTodaysEventWhenInitialSentAndOpenTooLong() {
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent todaysEvent = buildEvent(2L, startDate.plusDays(6), 100, "FINANCIAL_UPDATE");

    stubEvents(Map.of(startDate.plusDays(6), List.of(todaysEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(true);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(6));

    assertThat(result).containsExactly(todaysEvent);
  }

  @Test
  void evaluateSignal_sendsTodaysEventWhenInitialSentAndClosed() {
    signal.setSignalEndDate(startDate.plusDays(2));
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent todaysEvent = buildEvent(2L, startDate.plusDays(2), 100, "FINANCIAL_UPDATE");

    stubEvents(Map.of(startDate.plusDays(2), List.of(todaysEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(true);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(2));

    assertThat(result).containsExactly(todaysEvent);
  }

  @Test
  void evaluateSignal_sendsTodaysEventWhenInitialSentAndTodaysEventExists() {
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent todaysEvent = buildEvent(2L, startDate.plusDays(3), 100, "FINANCIAL_UPDATE");

    stubEvents(Map.of(startDate.plusDays(3), List.of(todaysEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(true);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(3));

    assertThat(result).containsExactly(todaysEvent);
  }

  @Test
  void evaluateSignal_sendsEarliestOverlimitWhenBalanceBreachedAndInitialNotSent() {
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent breachEvent = buildEvent(2L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");

    stubEvents(Map.of(startDate.plusDays(1), List.of(breachEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    assertThat(result).containsExactly(openEvent);
  }

  @Test
  void evaluateSignal_sendsTodaysEventWhenBalanceBreachedAndInitialAlreadySent() {
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent breachEvent = buildEvent(2L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");

    stubEvents(Map.of(startDate.plusDays(1), List.of(breachEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(true);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    assertThat(result).containsExactly(breachEvent);
  }

  @Test
  void evaluateSignal_sendsEarliestOverlimitWhenOpenTooLong() {
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate.plusDays(6), List.of()));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(6));

    assertThat(result).containsExactly(openEvent);
  }

  @Test
  void evaluateOverdueWithoutTodayEvents_returnsEarlyWhenSignalStartDateIsNull() {
    Signal signalWithoutStart = new Signal();
    signalWithoutStart.setSignalId(2L);
    signalWithoutStart.setSignalStartDate(null);

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(signalWithoutStart));

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(6));

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateOverdueWithoutTodayEvents_returnsEarlyWhenSignalStartDateAfterTarget() {
    Signal signalAfterTarget = new Signal();
    signalAfterTarget.setSignalId(2L);
    signalAfterTarget.setSignalStartDate(startDate.plusDays(1));

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(signalAfterTarget));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateOverdueWithoutTodayEvents_returnsEarlyWhenDpdNotExceedingThreshold() {
    Signal signalWithinThreshold = new Signal();
    signalWithinThreshold.setSignalId(2L);
    signalWithinThreshold.setSignalStartDate(startDate.minusDays(3));

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(signalWithinThreshold));
    when(signalEventPort.getEarliestOverlimitEvent(2L)).thenReturn(Optional.empty());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateOverdueWithoutTodayEvents_returnsEarlyWhenNoEarliestOverlimit() {
    Signal overdueSignal = new Signal();
    overdueSignal.setSignalId(2L);
    overdueSignal.setSignalStartDate(startDate.minusDays(10));

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(overdueSignal));
    when(signalEventPort.getEarliestOverlimitEvent(2L)).thenReturn(Optional.empty());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateOverdueWithoutTodayEvents_skipsWhenInitialAlreadySent() {
    Signal overdueSignal = new Signal();
    overdueSignal.setSignalId(2L);
    overdueSignal.setSignalStartDate(startDate.minusDays(10));
    SignalEvent openEvent = buildEvent(100L, startDate.minusDays(10), 3, "OVERLIMIT_SIGNAL");
    openEvent.setSignalId(2L);

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(overdueSignal));
    when(signalEventPort.getEarliestOverlimitEvent(2L)).thenReturn(Optional.of(openEvent));
    when(initialCehMappingPort.findInitialCehId(2L)).thenReturn(Optional.of("ceh-100"));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateOverdueWithoutTodayEvents_skipsWhenClosed() {
    Signal overdueSignal = new Signal();
    overdueSignal.setSignalId(2L);
    overdueSignal.setSignalStartDate(startDate.minusDays(10));
    overdueSignal.setSignalEndDate(startDate.minusDays(1));
    SignalEvent openEvent = buildEvent(100L, startDate.minusDays(10), 3, "OVERLIMIT_SIGNAL");
    openEvent.setSignalId(2L);

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(overdueSignal));
    when(signalEventPort.getEarliestOverlimitEvent(2L)).thenReturn(Optional.of(openEvent));
    when(initialCehMappingPort.findInitialCehId(2L)).thenReturn(Optional.empty());
    when(auditQueryPort.isEventSuccessful(100L, 1L)).thenReturn(false);

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void evaluateOverdueWithoutTodayEvents_sendsEarliestOverlimitWhenNotClosedAndNotSent() {
    Signal overdueSignal = new Signal();
    overdueSignal.setSignalId(2L);
    overdueSignal.setSignalStartDate(startDate.minusDays(10));
    SignalEvent openEvent = buildEvent(100L, startDate.minusDays(10), 3, "OVERLIMIT_SIGNAL");
    openEvent.setSignalId(2L);

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(overdueSignal));
    when(signalEventPort.getEarliestOverlimitEvent(2L)).thenReturn(Optional.of(openEvent));
    when(initialCehMappingPort.findInitialCehId(2L)).thenReturn(Optional.empty());
    when(auditQueryPort.isEventSuccessful(100L, 1L)).thenReturn(false);

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).containsExactly(openEvent);
  }

  @Test
  void selectTodaysEvent_handlesNullEventRecordDateTime() {
    SignalEvent event1 = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent event2 = buildEvent(2L, startDate, 251, "FINANCIAL_UPDATE");
    event2.setEventRecordDateTime(null);

    stubEvents(Map.of(startDate, List.of(event1, event2)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(event1));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, event1.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).containsExactly(event1);
  }

  @Test
  void selectTodaysEvent_handlesNullUabsEventId() {
    SignalEvent event1 = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent event2 = buildEvent(2L, startDate, 251, "FINANCIAL_UPDATE");
    event2.setUabsEventId(null);

    stubEvents(Map.of(startDate, List.of(event1, event2)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(event1));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, event1.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).containsExactly(event1);
  }

  @Test
  void isInitialAlreadySent_returnsTrueWhenCehMappingExists() {
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent todaysEvent = buildEvent(2L, startDate.plusDays(1), 251, "FINANCIAL_UPDATE");

    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    when(initialCehMappingPort.findInitialCehId(signalId)).thenReturn(Optional.of("ceh-123"));

    stubEvents(Map.of(startDate.plusDays(1), List.of(todaysEvent)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    // When CEH mapping exists, initial is already sent, so it should send today's event as follow-up
    assertThat(result).containsExactly(todaysEvent);
  }

  @Test
  void isInitialAlreadySent_returnsTrueWhenAuditShowsSuccess() {
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent todaysEvent = buildEvent(2L, startDate.plusDays(1), 251, "FINANCIAL_UPDATE");

    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    when(initialCehMappingPort.findInitialCehId(signalId)).thenReturn(Optional.empty());
    when(auditQueryPort.isEventSuccessful(1L, 1L)).thenReturn(true);

    stubEvents(Map.of(startDate.plusDays(1), List.of(todaysEvent)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    // When initial is already sent, it should send today's event as follow-up
    assertThat(result).containsExactly(todaysEvent);
  }

  @Test
  void isInitialAlreadySent_returnsFalseWhenNoMappingAndNoAudit() {
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent breachEvent = buildEvent(2L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");

    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    when(initialCehMappingPort.findInitialCehId(signalId)).thenReturn(Optional.empty());
    when(auditQueryPort.isEventSuccessful(1L, 1L)).thenReturn(false);

    stubEvents(Map.of(startDate.plusDays(1), List.of(breachEvent)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    assertThat(result).containsExactly(openEvent);
  }

  @Test
  void isInitialAlreadySent_handlesNullUabsEventId() {
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    openEvent.setUabsEventId(null);
    SignalEvent breachEvent = buildEvent(2L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");

    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    when(initialCehMappingPort.findInitialCehId(signalId)).thenReturn(Optional.empty());

    stubEvents(Map.of(startDate.plusDays(1), List.of(breachEvent)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    // When uabsEventId is null, isInitialAlreadySent returns false (because of null check)
    // So when balance is breached, it should send the earliest overlimit event
    assertThat(result).containsExactly(openEvent);
  }

  @Test
  void exceedsBalanceThreshold_handlesNullEvent() {
    stubEvents(Map.of(startDate, List.of()));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.empty());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void exceedsBalanceThreshold_handlesNullBalance() {
    SignalEvent event = buildEvent(1L, startDate, 0, "OVERLIMIT_SIGNAL");
    event.setUnauthorizedDebitBalance(null);

    stubEvents(Map.of(startDate, List.of(event)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(event));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, event.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void exceedsBalanceThreshold_handlesExactThreshold() {
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent exactThreshold = buildEvent(2L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");

    stubEvents(Map.of(startDate.plusDays(1), List.of(exactThreshold)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    assertThat(result).containsExactly(openEvent);
  }

  @Test
  void isClosureEvent_handlesNullEvent() {
    stubEvents(Map.of(startDate, List.of()));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.empty());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void isClosureEvent_handlesNullBalance() {
    SignalEvent event = buildEvent(1L, startDate, 0, "OVERLIMIT_SIGNAL");
    event.setUnauthorizedDebitBalance(null);

    stubEvents(Map.of(startDate, List.of(event)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(event));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, event.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void isClosed_handlesNullSignalEndDate() {
    signal.setSignalEndDate(null);
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent closureEvent = buildEvent(2L, startDate.plusDays(1), 0, "OUT_OF_OVERLIMIT");

    stubEvents(Map.of(startDate.plusDays(1), List.of(closureEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(1));

    assertThat(result).isEmpty();
  }

  @Test
  void isClosed_handlesNullTodaysEvent() {
    signal.setSignalEndDate(startDate.plusDays(2));
    SignalEvent openEvent = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate.plusDays(2), List.of()));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(2));

    assertThat(result).isEmpty();
  }

  @Test
  void calculateDpd_handlesSameStartAndTarget() {
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate, List.of(openEvent)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void calculateDpd_handlesStartAfterTarget() {
    signal.setSignalStartDate(startDate.plusDays(1));
    SignalEvent openEvent = buildEvent(1L, startDate, 3, "OVERLIMIT_SIGNAL");

    stubEvents(Map.of(startDate, List.of(openEvent)));
    when(signalPort.findBySignalId(signalId)).thenReturn(Optional.of(signal));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void selectEventsToSend_handlesNullSignalsInOverdueList() {
    when(signalPort.findByStartDateBefore(any())).thenReturn(new ArrayList<>(Arrays.asList(null, signal)));

    List<SignalEvent> result = selector.selectEventsToSend(startDate.plusDays(6));

    assertThat(result).isEmpty();
  }

  @Test
  void selectEventsToSend_handlesSignalsWithNullSignalIdInOverdueList() {
    Signal signalWithNullId = new Signal();
    signalWithNullId.setSignalId(null);
    signalWithNullId.setSignalStartDate(startDate.minusDays(10));

    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(signalWithNullId));

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }

  @Test
  void selectEventsToSend_skipsAlreadyProcessedSignalsInOverdueList() {
    SignalEvent event = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    Signal otherSignal = new Signal();
    otherSignal.setSignalId(2L);
    otherSignal.setSignalStartDate(startDate.minusDays(10));

    stubEvents(Map.of(startDate, List.of(event)));
    when(signalPort.findByStartDateBefore(any())).thenReturn(List.of(otherSignal));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(event));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, event.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).containsExactly(event);
  }

  // *****************************
  // FRESH TEST CASE
  // *****************************

  @Test
  void selectEventsToSend_filtersNullSignalIds() {
    SignalEvent event1 = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    SignalEvent event2 = buildEvent(2L, startDate, 250, "OVERLIMIT_SIGNAL");
    event2.setSignalId(null); // null signalId

    stubEvents(Map.of(startDate, List.of(event1, event2)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(event1));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, event1.getUabsEventId());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    // Should only process event1, event2 with null signalId is filtered out
    assertThat(result).hasSize(1);
  }

  @Test
  void evaluateSignal_returnsEarlyWhenEarliestOverlimitIsNull() {
    SignalEvent event = buildEvent(1L, startDate, 250, "OVERLIMIT_SIGNAL");
    stubEvents(Map.of(startDate, List.of(event)));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.empty());

    List<SignalEvent> result = selector.selectEventsToSend(startDate);

    assertThat(result).isEmpty();
  }
}
