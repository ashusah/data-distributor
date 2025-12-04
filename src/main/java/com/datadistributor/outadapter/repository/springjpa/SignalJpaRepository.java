package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for signals, supporting lookups by agreement id and by start date range.
 */
@Repository
public interface SignalJpaRepository extends JpaRepository<SignalJpaEntity, Long> {
  @Query("""
      select s from SignalJpaEntity s
      where s.agreementId = :agreementId
        and (s.signalEndDate is null or s.signalEndDate = :openEndDate)
      order by s.signalStartDate desc
      """)
  Optional<SignalJpaEntity> findOpenByAgreementId(@Param("agreementId") Long agreementId,
                                                  @Param("openEndDate") java.time.LocalDate openEndDate);

  Optional<SignalJpaEntity> findByAgreementIdAndSignalEndDate(Long agreementId,
                                                              java.time.LocalDate signalEndDate);

  java.util.List<SignalJpaEntity> findBySignalStartDateLessThanEqual(java.time.LocalDate date);
}
