package com.datadistributor.domain.inport;

import com.datadistributor.domain.SignalEvent;

import java.time.LocalDate;
import java.util.List;

/**
 * Use case for fetching signal events for processing or reporting.
 */
public interface SignalEventUseCase {
    List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date);
    List<SignalEvent> getAllSignalForCEH(LocalDate date);
}
