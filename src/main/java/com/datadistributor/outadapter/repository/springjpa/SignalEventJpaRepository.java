package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;

/**
 * Spring Data JPA repository for signal events with custom queries used by CEH flow and selector.
 */
@Repository
public interface SignalEventJpaRepository extends JpaRepository<SignalEventJpaEntity, Long> {
    List<SignalEventJpaEntity> findByEventRecordDateTimeBetween(LocalDateTime start, LocalDateTime end);
    List<SignalEventJpaEntity> findByUabsEventIdIn(List<Long> uabsEventIds);

    long countByEventRecordDateTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        select e
        from SignalEventJpaEntity e
        where e.eventRecordDateTime between :start and :end
          and e.unauthorizedDebitBalance >= :minUnauthorizedBalance
          and e.grv.reportCW014ToCEH = 'Y'
        order by e.uabsEventId asc
        """)
    List<SignalEventJpaEntity> findPageForCEH(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("minUnauthorizedBalance") long minUnauthorizedBalance,
                                              Pageable pageable);

    @Query("""
        select count(e)
        from SignalEventJpaEntity e
        where e.eventRecordDateTime between :start and :end
          and e.unauthorizedDebitBalance >= :minUnauthorizedBalance
          and e.grv.reportCW014ToCEH = 'Y'
        """)
    long countEligibleForCEH(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             @Param("minUnauthorizedBalance") long minUnauthorizedBalance);

    @Query("""
        select e
        from SignalEventJpaEntity e
        where e.signal.signalId = :signalId
          and e.eventRecordDateTime < :before
        order by e.eventRecordDateTime desc, e.uabsEventId desc
        """)
    List<SignalEventJpaEntity> findPreviousEvent(@Param("signalId") Long signalId,
                                                 @Param("before") LocalDateTime before,
                                                 Pageable pageable);

    @Query("""
        select e
        from SignalEventJpaEntity e
        where e.signal.signalId = :signalId
          and e.eventStatus = :eventStatus
        order by e.eventRecordDateTime asc, e.uabsEventId asc
        """)
    List<SignalEventJpaEntity> findBySignalIdAndEventStatusOrderByEventRecordDateTimeAsc(@Param("signalId") Long signalId, @Param("eventStatus") String eventStatus);
}
