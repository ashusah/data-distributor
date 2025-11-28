package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SignalEventJpaRepository extends JpaRepository<SignalEventJpaEntity, Long> {
    List<SignalEventJpaEntity> findEventRecordDateTimeBetween(LocalDateTime start, LocalDateTime end);
}
