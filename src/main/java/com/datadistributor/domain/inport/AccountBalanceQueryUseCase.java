package com.datadistributor.domain.inport;

import java.util.Optional;

/**
 * Use case to fetch balance-related metadata for an agreement.
 */
public interface AccountBalanceQueryUseCase {

  /**
   * @return optional BC number for the given agreement id.
   */
  Optional<Long> findBcNumberByAgreementId(Long agreementId);
}
