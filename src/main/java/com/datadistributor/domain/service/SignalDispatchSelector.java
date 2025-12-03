package com.datadistributor.domain.service;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.outport.SignalPort;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SignalDispatchSelector implements SignalDispatchSelectorUseCase {

  private final SignalEventRepository signalEventRepository;
  private final SignalPort signalPort;
  private final SignalAuditQueryPort auditQueryPort;
  private final InitialCehMappingPort initialCehMappingPort;
  private final long balanceThreshold;
  private final long daysOpenThreshold;
  private final long auditConsumerId;

  @Override
  public List<SignalEvent> selectEventsToSend(LocalDate targetDate) {
    List<SignalEvent> todaysEvents = signalEventRepository.getAllSignalEventsOfThisDate(targetDate);
    Map<Long, List<SignalEvent>> eventsBySignal = todaysEvents.stream()
        .filter(e -> e.getSignalId() != null)
        .collect(Collectors.groupingBy(SignalEvent::getSignalId));

    List<SignalEvent> toSend = new ArrayList<>();
    // evaluate signals that have events today
    eventsBySignal.forEach((signalId, events) ->
        evaluateSignal(signalId, targetDate, events, toSend));

    // evaluate overdue signals even if no event today
    Set<Long> alreadyProcessed = eventsBySignal.keySet();
    LocalDate overdueCutoff = targetDate.minusDays(daysOpenThreshold);
    List<Signal> overdueSignals = signalPort.findByStartDateBefore(overdueCutoff);
    for (Signal signal : overdueSignals) {
      if (signal == null || signal.getSignalId() == null || alreadyProcessed.contains(signal.getSignalId())) {
        continue;
      }
      evaluateOverdueWithoutTodayEvents(signal, targetDate, toSend);
    }

    return toSend;
  }

  private void evaluateSignal(Long signalId,
                              LocalDate targetDate,
                              List<SignalEvent> todaysEvents,
                              List<SignalEvent> collector) {
    Optional<Signal> signalOpt = signalPort.findBySignalId(signalId);
    if (signalOpt.isEmpty() || signalOpt.get().getSignalStartDate() == null
        || signalOpt.get().getSignalStartDate().isAfter(targetDate)) {
      return;
    }

    Signal signal = signalOpt.get();
    SignalEvent earliestOverlimit = signalEventRepository.getEarliestOverlimitEvent(signalId).orElse(null);
    if (earliestOverlimit == null) {
      return;
    }

    long dpd = calculateDpd(signal.getSignalStartDate(), targetDate);
    SignalEvent todaysEvent = selectTodaysEvent(todaysEvents);
    boolean closed = isClosed(signal, targetDate, todaysEvent);
    boolean initialAlreadySent = isInitialAlreadySent(signalId, earliestOverlimit);
    boolean balanceBreached = exceedsBalanceThreshold(todaysEvent);
    boolean openTooLong = dpd > daysOpenThreshold;

    if (!initialAlreadySent && closed) {
      if (isClosureEvent(todaysEvent)) {
        // Closed path with closure event today and no breaches.
        return;
      }
      if (!balanceBreached) {
        // Closed without breach -> skip
        return;
      }
    }

    if (initialAlreadySent) {
      if (shouldSendFollowUp(todaysEvent, openTooLong, closed)) {
        collector.add(todaysEvent);
      }
      return;
    }

    if (closed && dpd <= daysOpenThreshold && !balanceBreached && !openTooLong) {
      // closed quickly without ever breaching a rule
      return;
    }

    if (balanceBreached || openTooLong) {
      collector.add(earliestOverlimit);
    }
  }

  private long calculateDpd(LocalDate start, LocalDate target) {
    long days = ChronoUnit.DAYS.between(start, target) + 1; // DPD1 on start date
    return Math.max(days, 0);
  }

  private SignalEvent selectTodaysEvent(List<SignalEvent> todaysEvents) {
    return todaysEvents.stream()
        .max(Comparator.comparing(SignalEvent::getEventRecordDateTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(SignalEvent::getUabsEventId, Comparator.nullsLast(Comparator.naturalOrder())))
        .orElse(null);
  }

  private boolean isClosed(Signal signal, LocalDate targetDate, SignalEvent todaysEvent) {
    boolean closedInSignal = signal.getSignalEndDate() != null && !signal.getSignalEndDate().isAfter(targetDate);
    boolean closureEventToday = todaysEvent != null
        && todaysEvent.getUnauthorizedDebitBalance() != null
        && todaysEvent.getUnauthorizedDebitBalance() == 0L;
    return closedInSignal || closureEventToday;
  }

  private boolean isInitialAlreadySent(Long signalId, SignalEvent earliestOverlimit) {
    if (initialCehMappingPort.findInitialCehId(signalId).isPresent()) {
      return true;
    }
    return earliestOverlimit.getUabsEventId() != null
        && auditQueryPort.isEventSuccessful(earliestOverlimit.getUabsEventId(), auditConsumerId);
  }

  private boolean exceedsBalanceThreshold(SignalEvent event) {
    return event != null
        && event.getUnauthorizedDebitBalance() != null
        && event.getUnauthorizedDebitBalance() >= balanceThreshold;
  }

  private boolean isClosureEvent(SignalEvent event) {
    return event != null
        && event.getUnauthorizedDebitBalance() != null
        && event.getUnauthorizedDebitBalance() == 0L;
  }

  private boolean shouldSendFollowUp(SignalEvent todaysEvent, boolean openTooLong, boolean closed) {
    if (todaysEvent == null) {
      return false;
    }
    boolean balanceTrigger = exceedsBalanceThreshold(todaysEvent);
    boolean closure = isClosureEvent(todaysEvent);
    return balanceTrigger || openTooLong || closed || closure;
  }

  private void evaluateOverdueWithoutTodayEvents(Signal signal,
                                                 LocalDate targetDate,
                                                 List<SignalEvent> collector) {
    if (signal.getSignalStartDate() == null || signal.getSignalStartDate().isAfter(targetDate)) {
      return;
    }
    long dpd = calculateDpd(signal.getSignalStartDate(), targetDate);
    if (dpd <= daysOpenThreshold) {
      return;
    }

    SignalEvent earliestOverlimit = signalEventRepository.getEarliestOverlimitEvent(signal.getSignalId()).orElse(null);
    if (earliestOverlimit == null) {
      return;
    }

    boolean initialAlreadySent = isInitialAlreadySent(signal.getSignalId(), earliestOverlimit);
    boolean closed = signal.getSignalEndDate() != null && !signal.getSignalEndDate().isAfter(targetDate);
    if (!initialAlreadySent && !closed) {
      collector.add(earliestOverlimit);
    }
  }
}
