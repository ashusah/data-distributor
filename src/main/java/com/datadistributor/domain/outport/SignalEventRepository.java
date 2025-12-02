package com.datadistributor.domain.outport;

import com.datadistributor.domain.SignalEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SignalEventRepository {
    List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date);
    List<SignalEvent> getSignalEventsForCEH(LocalDate date, int page, int size);
    long countSignalEventsForCEH(LocalDate date);
    Optional<SignalEvent> getPreviousEvent(Long signalId, java.time.LocalDateTime before);
}
