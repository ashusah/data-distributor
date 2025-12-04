package com.datadistributor.domain.service;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.outport.SignalEventPort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Domain facade around the signal event repository that enforces paging limits and delegates
 * queries used by downstream batch processing.
 */
public class SignalEventDomainService implements SignalEventUseCase {

    private final SignalEventPort signalEventRepository;
    private final int pageSize;

    public SignalEventDomainService(SignalEventPort signalEventRepository, int pageSize) {
        this.signalEventRepository = signalEventRepository;
        this.pageSize = Math.max(1, pageSize);
    }


    @Override
    public List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) {
        return signalEventRepository.getAllSignalEventsOfThisDate(date);
    }

    @Override
    public List<SignalEvent> getAllSignalForCEH(LocalDate date) {
        List<SignalEvent> all = new ArrayList<>();
        for (int page = 0; ; page++) {
            List<SignalEvent> chunk = signalEventRepository.getSignalEventsForCEH(date, page, pageSize);
            if (chunk.isEmpty()) {
                break;
            }
            all.addAll(chunk);
        }
        return all;
    }
}
