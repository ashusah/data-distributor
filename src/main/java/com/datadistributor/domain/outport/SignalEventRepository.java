package com.datadistributor.domain.outport;

import com.datadistributor.domain.SignalEvent;

import java.time.LocalDate;
import java.util.List;

public interface SignalEventRepository {
    List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date);
    List<SignalEvent> getSignalEventsForCEH(LocalDate date, int page, int size);
    long countSignalEventsForCEH(LocalDate date);
}
