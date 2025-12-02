package com.datadistributor.domain.outport;

public interface InitialCehMappingPort {

  void saveInitialCehMapping(Long signalId, long cehId);

  java.util.Optional<String> findInitialCehId(Long signalId);
}
