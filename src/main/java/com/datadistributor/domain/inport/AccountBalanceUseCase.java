package com.datadistributor.domain.inport;

import com.datadistributor.domain.AccountBalance;
import java.util.Optional;

/**
 * Use case to fetch balance-related data for an agreement.
 */
public interface AccountBalanceUseCase {

  /**
   * @return optional BC number for the given agreement id.
   */
  Optional<Long> findBcNumberByAgreementId(Long agreementId);

  /**
   * @return full account balance view for the given agreement id.
   */
  AccountBalance getAccountBalanceOfAgreement(Long agreementId);
}
