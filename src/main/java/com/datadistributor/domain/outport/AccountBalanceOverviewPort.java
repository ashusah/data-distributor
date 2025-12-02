package com.datadistributor.domain.outport;

import java.util.Optional;

public interface AccountBalanceOverviewPort {

  Optional<Long> findBcNumberByAgreementId(Long agreementId);
}
