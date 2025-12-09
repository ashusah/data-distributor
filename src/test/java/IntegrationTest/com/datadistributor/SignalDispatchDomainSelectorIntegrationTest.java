package com.datadistributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventPort;
import com.datadistributor.domain.outport.SignalPort;
import com.datadistributor.domain.service.SignalDispatchDomainSelector;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = SignalDispatchDomainSelectorIntegrationTest.TestConfig.class)
@ActiveProfiles("test")
class SignalDispatchDomainSelectorIntegrationTest {

  @MockBean private SignalEventPort signalEventPort;
  @MockBean private SignalPort signalPort;
  @MockBean private SignalAuditQueryPort auditQueryPort;
  @MockBean private InitialCehMappingPort initialCehMappingPort;
  @Autowired private SignalDispatchSelectorUseCase selectorUseCase;

  private final LocalDate startDate = LocalDate.of(2025, 1, 1);
  private final long signalId = 1L;

  private Signal signal;

  @BeforeEach
  void setup() {
    signal = new Signal();
    signal.setSignalId(signalId);
    signal.setSignalStartDate(startDate);
    signal.setSignalEndDate(startDate.plusDays(30));
    when(signalPort.findBySignalId(signalId)).thenReturn(Optional.of(signal));
    when(signalPort.findByStartDateBefore(any())).thenAnswer(inv -> {
      LocalDate cutoff = inv.getArgument(0);
      if (cutoff == null) {
        return List.of();
      }
      if (!signal.getSignalStartDate().isAfter(cutoff)) {
        return List.of(signal);
      }
      return List.of();
    });
  }

  @Test
  void overdueOpenEventSentOnDPD6_viaSpringContext() {
    SignalEvent openEvent = buildEvent(101L, startDate, 3, "OVERLIMIT_SIGNAL");
    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(5), List.of(),
        startDate.plusDays(6), List.of()
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    assertThat(selectorUseCase.selectEventsToSend(startDate.plusDays(4))).isEmpty();
    assertThat(selectorUseCase.selectEventsToSend(startDate.plusDays(5))).containsExactly(openEvent);
  }

  @Test
  void breachFlow_sendsOpenThenSubsequentEvents_viaSpringContext() {
    SignalEvent openEvent = buildEvent(111L, startDate, 3, "OVERLIMIT_SIGNAL");
    SignalEvent breach = buildEvent(112L, startDate.plusDays(1), 250, "FINANCIAL_UPDATE");
    SignalEvent later = buildEvent(113L, startDate.plusDays(3), 260, "FINANCIAL_UPDATE");

    stubEvents(Map.of(
        startDate, List.of(openEvent),
        startDate.plusDays(1), List.of(breach),
        startDate.plusDays(3), List.of(later)
    ));
    when(signalEventPort.getEarliestOverlimitEvent(signalId)).thenReturn(Optional.of(openEvent));
    AtomicBoolean openSent = new AtomicBoolean(false);
    stubInitialState(openSent, openEvent.getUabsEventId());

    assertThat(selectorUseCase.selectEventsToSend(startDate.plusDays(1))).containsExactly(openEvent);
    openSent.set(true);
    assertThat(selectorUseCase.selectEventsToSend(startDate.plusDays(3))).containsExactly(later);
  }

  private void stubEvents(Map<LocalDate, List<SignalEvent>> eventsByDate) {
    when(signalEventPort.getAllSignalEventsOfThisDate(any())).thenAnswer(inv -> {
      LocalDate day = inv.getArgument(0);
      return eventsByDate.getOrDefault(day, List.of());
    });
  }

  private void stubInitialState(AtomicBoolean openSent, Long openEventId) {
    when(initialCehMappingPort.findInitialCehId(signalId)).thenAnswer(inv ->
        openSent.get() ? Optional.of("ceh-" + openEventId) : Optional.empty());
    when(auditQueryPort.isEventSuccessful(eq(openEventId), anyLong())).thenAnswer(inv -> openSent.get());
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

  @Configuration
  static class TestConfig {
    @Bean
    static SignalDispatchSelectorUseCase signalDispatchSelectorUseCase(
        SignalEventPort signalEventPort,
        SignalPort signalPort,
        SignalAuditQueryPort auditQueryPort,
        InitialCehMappingPort initialCehMappingPort) {
      return new SignalDispatchDomainSelector(
          signalEventPort,
          signalPort,
          auditQueryPort,
          initialCehMappingPort,
          250L,
          5L,
          1L);
    }
  }
}
