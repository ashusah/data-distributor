package com.datadistributor.domain.inport;

import com.datadistributor.domain.Signal;
import java.util.Optional;

/**
 * Use case for retrieving signal metadata by ids.
 */
public interface SignalUseCase {
  Optional<Signal> findBySignalId(Long signalId);
  Optional<Signal> getOpenSignalOfAgreement(Long agreementId);
}
