package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

@Repository
public interface SignalEventJpaRepository extends JpaRepository<SignalEventJpaEntity, Long> {
    List<SignalEventJpaEntity> findByEventRecordDateTimeBetween(LocalDateTime start, LocalDateTime end);

    long countByEventRecordDateTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query("""
        select e
        from SignalEventJpaEntity e
        where e.eventRecordDateTime between :start and :end
          and (e.unauthorizedDebitBalance > :minUnauthorizedBalance
               or e.bookDate = :bookDateTarget)
        order by e.uabsEventId asc
        """)
    List<SignalEventJpaEntity> findPageForCEH(@Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end,
                                              @Param("minUnauthorizedBalance") long minUnauthorizedBalance,
                                              @Param("bookDateTarget") LocalDate bookDateTarget,
                                              Pageable pageable);

    @Query("""
        select count(e)
        from SignalEventJpaEntity e
        where e.eventRecordDateTime between :start and :end
          and (e.unauthorizedDebitBalance > :minUnauthorizedBalance
               or e.bookDate = :bookDateTarget)
        """)
    long countEligibleForCEH(@Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             @Param("minUnauthorizedBalance") long minUnauthorizedBalance,
                             @Param("bookDateTarget") LocalDate bookDateTarget);

    @Query("""
        select e
        from SignalEventJpaEntity e
        where e.signalId = :signalId
          and e.eventRecordDateTime < :before
        order by e.eventRecordDateTime desc, e.uabsEventId desc
        """)
    List<SignalEventJpaEntity> findPreviousEvent(@Param("signalId") Long signalId,
                                                 @Param("before") LocalDateTime before,
                                                 Pageable pageable);

    Optional<SignalEventJpaEntity> findFirstBySignalIdAndEventStatusOrderByEventRecordDateTimeAsc(Long signalId, String eventStatus);
}
