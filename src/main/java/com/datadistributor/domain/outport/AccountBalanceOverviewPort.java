package com.datadistributor.domain.outport;

import com.datadistributor.domain.AccountBalanceOverview;
import java.util.Optional;

/**
 * Persistence port for account balance overview data.
 */
public interface AccountBalanceOverviewPort {

  Optional<Long> findBcNumberByAgreementId(Long agreementId);

  Optional<AccountBalanceOverview> findByAgreementId(Long agreementId);
}
