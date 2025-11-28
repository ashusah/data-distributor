package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public class SignalEventRepositoryAdapter implements SignalEventRepository {
    private final SignalEventJpaRepository signalEventJpaRepository;
    private final SignalEventMapper signalEventMapper;
    public SignalEventRepositoryAdapter(SignalEventJpaRepository signalEventJpaRepository, SignalEventMapper signalEventMapper) {
        this.signalEventJpaRepository = signalEventJpaRepository;
        this.signalEventMapper = signalEventMapper;
    }

    @Override
    public List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<SignalEventJpaEntity> eventEntities = signalEventJpaRepository.findEventRecordDateTimeBetween(start, end);
        return signalEventMapper.toDomain(eventEntities);
    }
}
