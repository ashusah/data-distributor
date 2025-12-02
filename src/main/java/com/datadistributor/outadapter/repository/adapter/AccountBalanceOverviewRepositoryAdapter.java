package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceOverviewJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountBalanceOverviewRepositoryAdapter implements AccountBalanceOverviewPort {

  private final AccountBalanceOverviewJpaRepository jpaRepository;

  @Override
  public Optional<Long> findBcNumberByAgreementId(Long agreementId) {
    if (agreementId == null) {
      return Optional.empty();
    }
    return jpaRepository.findById(agreementId).map(entity -> entity.getBcNumber());
  }
}
