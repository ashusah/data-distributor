package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.SignalJpaEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignalJpaRepository extends JpaRepository<SignalJpaEntity, Long> {
  Optional<SignalJpaEntity> findByAgreementId(Long agreementId);
  java.util.List<SignalJpaEntity> findBySignalStartDateLessThanEqual(java.time.LocalDate date);
}
