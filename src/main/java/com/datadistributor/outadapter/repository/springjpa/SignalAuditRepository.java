package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data JPA repository for signal audit records.
 */
@Repository
public interface SignalAuditRepository extends JpaRepository<SignalAuditJpaEntity, Long> {

  Optional<SignalAuditJpaEntity> findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(Long uabsEventId, Long consumerId);

  Optional<SignalAuditJpaEntity> findTopByUabsEventIdAndConsumerIdOrderByAuditRecordDateTimeDesc(
      Long uabsEventId, Long consumerId);

  List<SignalAuditJpaEntity> findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(
      LocalDateTime start,
      LocalDateTime end,
      Long consumerId);
}
