package com.datadistributor.domain.inport;

import com.datadistributor.domain.SignalEvent;

public interface InitialCehMappingUseCase {

  void handleInitialCehMapping(SignalEvent event, long cehId);
}
