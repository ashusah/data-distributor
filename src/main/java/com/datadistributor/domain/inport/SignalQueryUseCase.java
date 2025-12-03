package com.datadistributor.domain.inport;

import com.datadistributor.domain.Signal;
import java.util.Optional;

public interface SignalQueryUseCase {
  Optional<Signal> findBySignalId(Long signalId);
  Optional<Signal> findByAgreementId(Long agreementId);
}
