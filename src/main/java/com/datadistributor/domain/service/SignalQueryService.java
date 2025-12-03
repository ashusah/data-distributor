package com.datadistributor.domain.service;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.inport.SignalQueryUseCase;
import com.datadistributor.domain.outport.SignalPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SignalQueryService implements SignalQueryUseCase {

  private final SignalPort port;

  @Override
  public Optional<Signal> findBySignalId(Long signalId) {
    return port.findBySignalId(signalId);
  }

  @Override
  public Optional<Signal> findByAgreementId(Long agreementId) {
    return port.findByAgreementId(agreementId);
  }
}
