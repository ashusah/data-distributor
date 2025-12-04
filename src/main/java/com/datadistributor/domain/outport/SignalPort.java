package com.datadistributor.domain.outport;

import com.datadistributor.domain.Signal;
import java.util.Optional;

/**
 * Persistence port for signals.
 */
public interface SignalPort {
  Optional<Signal> findBySignalId(Long signalId);
  Optional<Signal> getOpenSignalOfAgreement(Long agreementId);
  java.util.List<Signal> findByStartDateBefore(java.time.LocalDate date);
}
