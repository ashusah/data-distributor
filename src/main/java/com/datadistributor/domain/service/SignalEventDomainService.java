package com.datadistributor.domain.service;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.outport.SignalEventRepository;

import java.time.LocalDate;
import java.util.List;

public class SignalEventDomainService implements SignalEventUseCase {

    private final SignalEventRepository signalEventRepository;

    public SignalEventDomainService(SignalEventRepository signalEventRepository) {
        this.signalEventRepository = signalEventRepository;
    }


    @Override
    public List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) {
        return signalEventRepository.getAllSignalEventsOfThisDate(date);
    }
}
