package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.outport.SignalPort;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Repository adapter that wraps Spring Data JPA access for signals and exposes them via the domain
 * {@link SignalPort}. Ensures domain only sees mapped aggregates.
 */
@Repository
@RequiredArgsConstructor
public class SignalRepositoryAdapter implements SignalPort {

  private final SignalJpaRepository repository;
  private final SignalMapper mapper;

  @Override
  public Optional<Signal> findBySignalId(Long signalId) {
    if (signalId == null) return Optional.empty();
    Optional<SignalJpaEntity> entity = repository.findById(signalId);
    return entity.map(mapper::toDomain);
  }

  @Override
  public Optional<Signal> getOpenSignalOfAgreement(Long agreementId) {
    if (agreementId == null) return Optional.empty();
    return repository.findOpenByAgreementId(agreementId, java.time.LocalDate.of(9999, 12, 31))
        .map(mapper::toDomain);
  }

  @Override
  public java.util.List<Signal> findByStartDateBefore(java.time.LocalDate date) {
    if (date == null) return java.util.List.of();
    return repository.findBySignalStartDateLessThanEqual(date).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
