package com.datadistributor.domain.outport;

public interface InitialCehMappingPort {

  void saveInitialCehMapping(Long signalId, long cehId);
}
