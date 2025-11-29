package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
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
        List<SignalEventJpaEntity> eventEntities = signalEventJpaRepository.findByEventRecordDateTimeBetween(start, end);
        return signalEventMapper.toDomain(eventEntities);
    }

    @Override
    public List<SignalEvent> getSignalEventsForCEH(LocalDate date, int page, int size) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        List<SignalEventJpaEntity> eventEntities = signalEventJpaRepository.findPageForCEH(
            start, end, PageRequest.of(page, size));
        return signalEventMapper.toDomain(eventEntities);
    }

    @Override
    public long countSignalEventsForCEH(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        return signalEventJpaRepository.countByEventRecordDateTimeBetween(start, end);
    }
}
