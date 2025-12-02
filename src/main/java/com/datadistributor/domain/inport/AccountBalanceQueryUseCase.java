package com.datadistributor.domain.inport;

import java.util.Optional;

public interface AccountBalanceQueryUseCase {

  Optional<Long> findBcNumberByAgreementId(Long agreementId);
}
