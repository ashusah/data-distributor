package com.datadistributor.domain.outport;

public interface InitialCehMappingPort {

  /**
   * Persists the CEH event id for a given signal.
   */
  void saveInitialCehMapping(Long signalId, long cehId);

  java.util.Optional<String> findInitialCehId(Long signalId);
}
