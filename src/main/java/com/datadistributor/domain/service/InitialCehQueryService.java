package com.datadistributor.domain.service;

import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InitialCehQueryService implements InitialCehQueryUseCase {

  private final InitialCehMappingPort port;

  @Override
  public Optional<String> findInitialCehId(Long signalId) {
    if (signalId == null) {
      return Optional.empty();
    }
    return port.findInitialCehId(signalId);
  }
}
