package com.datadistributor.domain.service;

import com.datadistributor.domain.AccountBalance;
import com.datadistributor.domain.inport.AccountBalanceUseCase;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Domain service delegating account balance lookups to the underlying port.
 */
@RequiredArgsConstructor
public class AccountBalanceDomainService implements AccountBalanceUseCase {

  private final AccountBalanceOverviewPort port;

  @Override
  public Optional<Long> findBcNumberByAgreementId(Long agreementId) {
    if (agreementId == null) {
      return Optional.empty();
    }
    return port.findBcNumberByAgreementId(agreementId);
  }

  @Override
  public Optional<AccountBalance> getAccountBalanceOfAgreement(Long agreementId) {
    if (agreementId == null) {
      return Optional.empty();
    }
    return port.findByAgreementId(agreementId);
  }
}
