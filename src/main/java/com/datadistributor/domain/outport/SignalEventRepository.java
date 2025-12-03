package com.datadistributor.domain.outport;

import com.datadistributor.domain.SignalEvent;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Persistence port for signal events with domain-specific queries used by the selector and flow.
 */
public interface SignalEventRepository {
    /**
     * All events whose record date/time is within the given day.
     */
    List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date);

    /**
     * Eligible events for CEH using business filters (balance threshold, book-date lookback).
     */
    List<SignalEvent> getSignalEventsForCEH(LocalDate date, int page, int size);

    long countSignalEventsForCEH(LocalDate date);

    /**
     * Immediately prior event (same signal) before the given timestamp, if any.
     */
    Optional<SignalEvent> getPreviousEvent(Long signalId, java.time.LocalDateTime before);

    /**
     * Earliest OVERLIMIT event for a signal (used for initial/overdue dispatch).
     */
    Optional<SignalEvent> getEarliestOverlimitEvent(Long signalId);

    /**
     * Resolve events by id preserving the input order where possible.
     */
    List<SignalEvent> findByUabsEventIdIn(List<Long> uabsEventIds);
}
