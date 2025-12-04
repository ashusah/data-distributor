package com.datadistributor.domain.service;

import com.datadistributor.domain.inport.AccountBalanceQueryUseCase;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Domain service delegating account balance lookups to the underlying port.
 */
@RequiredArgsConstructor
public class AccountBalanceQueryService implements AccountBalanceQueryUseCase {

  private final AccountBalanceOverviewPort port;

  @Override
  public Optional<Long> findBcNumberByAgreementId(Long agreementId) {
    if (agreementId == null) {
      return Optional.empty();
    }
    return port.findBcNumberByAgreementId(agreementId);
  }
}
