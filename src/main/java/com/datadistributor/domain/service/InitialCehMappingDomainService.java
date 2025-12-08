package com.datadistributor.domain.service;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
/**
 * Persists the initial CEH event id for a signal when the first OVERLIMIT event is delivered.
 */
public class InitialCehMappingDomainService implements InitialCehMappingUseCase {

  private static final String OVERLIMIT_STATUS = "OVERLIMIT_SIGNAL";
  private final InitialCehMappingPort initialCehMappingPort;

  @Override
  public void handleInitialCehMapping(SignalEvent event, long cehId) {
    if (event == null || event.getEventStatus() == null) {
      return;
    }
    if (!OVERLIMIT_STATUS.equalsIgnoreCase(event.getEventStatus())) {
      return;
    }
    if (event.getSignalId() == null) {
      return;
    }
    // Check if mapping already exists - if it does, skip (idempotent)
    if (initialCehMappingPort.findInitialCehId(event.getSignalId()).isPresent()) {
      log.debug("Initial CEH mapping already exists for signalId={}, skipping", event.getSignalId());
      return;
    }
    try {
      // saveInitialCehMapping is now idempotent and handles duplicates
      initialCehMappingPort.saveInitialCehMapping(event.getSignalId(), cehId);
      log.debug("Initial CEH mapping persisted for signalId={} cehId={}", event.getSignalId(), cehId);
    } catch (Exception ex) {
      log.error("LOG002- Initial CEH mapping for signalId={} failed to be persisted: {}", event.getSignalId(), ex.toString(), ex);
    }
  }
}
