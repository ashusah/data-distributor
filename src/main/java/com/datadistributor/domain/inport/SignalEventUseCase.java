package com.datadistributor.domain.inport;

import com.datadistributor.domain.SignalEvent;

import java.time.LocalDate;
import java.util.List;

public interface SignalEventUseCase {
    List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date);
    List<SignalEvent> getAllSignalForCEH(LocalDate date);
}