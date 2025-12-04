package com.datadistributor.domain.inport;

import com.datadistributor.domain.SignalEvent;

/**
 * Use case to persist initial CEH mapping once a signal event is accepted by CEH.
 */
public interface InitialCehMappingUseCase {

  /**
   * Stores the CEH event id associated with the signal to avoid duplicate initial sends.
   */
  void handleInitialCehMapping(SignalEvent event, long cehId);
}
