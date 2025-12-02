package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
public class SignalEventRepositoryAdapter implements SignalEventRepository {
    private static final long MIN_UNAUTHORIZED_DEBIT_BALANCE = 250L;
    private static final int BOOK_DATE_LOOKBACK_DAYS = 5;

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
        LocalDate bookDateTarget = date.minusDays(BOOK_DATE_LOOKBACK_DAYS);
        List<SignalEventJpaEntity> eventEntities = signalEventJpaRepository.findPageForCEH(
            start, end, MIN_UNAUTHORIZED_DEBIT_BALANCE, bookDateTarget, PageRequest.of(page, size));
        return signalEventMapper.toDomain(eventEntities);
    }

    @Override
    public long countSignalEventsForCEH(LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);
        LocalDate bookDateTarget = date.minusDays(BOOK_DATE_LOOKBACK_DAYS);
        return signalEventJpaRepository.countEligibleForCEH(
            start, end, MIN_UNAUTHORIZED_DEBIT_BALANCE, bookDateTarget);
    }

    @Override
    public Optional<SignalEvent> getPreviousEvent(Long signalId, LocalDateTime before) {
        if (signalId == null || before == null) {
            return Optional.empty();
        }
        List<SignalEventJpaEntity> results = signalEventJpaRepository.findPreviousEvent(
            signalId, before, PageRequest.of(0, 1));
        if (results.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(signalEventMapper.toDomain(results.get(0)));
    }
}
