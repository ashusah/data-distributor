package com.datadistributor.domain.service;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.inport.SignalUseCase;
import com.datadistributor.domain.outport.SignalPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

/**
 * Domain service delegating signal lookups to the configured port implementation.
 */
@RequiredArgsConstructor
public class SignalQueryDomainService implements SignalUseCase {

  private final SignalPort port;

  @Override
  public Optional<Signal> findBySignalId(Long signalId) {
    return port.findBySignalId(signalId);
  }

  @Override
  public Optional<Signal> getOpenSignalOfAgreement(Long agreementId) {
    return port.getOpenSignalOfAgreement(agreementId);
  }
}
