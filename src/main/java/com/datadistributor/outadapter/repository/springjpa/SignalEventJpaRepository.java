package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

@Repository
public interface SignalEventJpaRepository extends JpaRepository<SignalEventJpaEntity, Long> {
    List<SignalEventJpaEntity> findByEventRecordDateTimeBetween(LocalDateTime start, LocalDateTime end);

    //Codex : Implement Option for a customer query
    @Query("""
        select e
        from SignalEventJpaEntity e
        where e.eventRecordDateTime between :start and :end
        order by e.eventRecordDateTime asc
        """)
    List<SignalEventJpaEntity> findAllEventsForCEH(@Param("start") LocalDateTime start,
                                                   @Param("end") LocalDateTime end);

    long countByEventRecordDateTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        select e
        from SignalEventJpaEntity e
        where e.eventRecordDateTime between :start and :end
        order by e.eventRecordDateTime asc
        """)
    List<SignalEventJpaEntity> findPageForCEH(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              Pageable pageable);
}
