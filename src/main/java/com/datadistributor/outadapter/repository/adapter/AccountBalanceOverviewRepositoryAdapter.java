package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.AccountBalance;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Adapter that exposes account balance lookups to the domain layer while delegating persistence to
 * Spring Data JPA. Keeps the domain insulated from JPA-specific concerns.
 */
@Repository
@RequiredArgsConstructor
public class AccountBalanceOverviewRepositoryAdapter implements AccountBalanceOverviewPort {

  private final AccountBalanceJpaRepository jpaRepository;
  private final AccountBalanceMapper accountBalanceMapper;

  @Override
  public Optional<Long> findBcNumberByAgreementId(Long agreementId) {
    if (agreementId == null) {
      return Optional.empty();
    }
    return jpaRepository.findById(agreementId).map(AccountBalanceJpaEntity::getBcNumber);
  }

  @Override
  public Optional<AccountBalance> getAccountBalanceOfAgreement(Long agreementId) {
    if (agreementId == null) {
      return Optional.empty();
    }
    return jpaRepository.findById(agreementId).map(accountBalanceMapper::toDomain);
  }
}
