package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface SignalAuditRepository extends JpaRepository<SignalAuditJpaEntity, Long> {

  Optional<SignalAuditJpaEntity> findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(Long uabsEventId, Long consumerId);
}
