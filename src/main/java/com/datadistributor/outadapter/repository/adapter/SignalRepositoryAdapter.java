package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.outport.SignalPort;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

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
  public Optional<Signal> findByAgreementId(Long agreementId) {
    if (agreementId == null) return Optional.empty();
    return repository.findByAgreementId(agreementId).map(mapper::toDomain);
  }

  @Override
  public java.util.List<Signal> findByStartDateBefore(java.time.LocalDate date) {
    if (date == null) return java.util.List.of();
    return repository.findBySignalStartDateLessThanEqual(date).stream()
        .map(mapper::toDomain)
        .toList();
  }
}
