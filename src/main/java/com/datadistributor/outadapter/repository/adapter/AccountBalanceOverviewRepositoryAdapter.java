package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceOverviewJpaRepository;
import com.datadistributor.domain.AccountBalanceOverview;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Adapter that exposes account balance overview lookups to the domain layer while delegating
 * persistence to Spring Data JPA. Keeps the domain insulated from JPA-specific concerns.
 */
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

  @Override
  public Optional<AccountBalanceOverview> findByAgreementId(Long agreementId) {
    if (agreementId == null) {
      return Optional.empty();
    }
    return jpaRepository.findById(agreementId).map(entity -> {
      AccountBalanceOverview abo = new AccountBalanceOverview();
      abo.setAgreementId(entity.getAgreementId());
      abo.setGrv(entity.getGrv());
      abo.setIban(entity.getIban());
      abo.setBcNumber(entity.getBcNumber());
      abo.setCurrencyCode(entity.getCurrencyCode());
      abo.setBookDate(entity.getBookDate());
      return abo;
    });
  }
}
