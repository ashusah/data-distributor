package com.datadistributor.domain.outport;

import com.datadistributor.domain.Signal;
import java.util.Optional;

public interface SignalPort {
  Optional<Signal> findBySignalId(Long signalId);
  Optional<Signal> findByAgreementId(Long agreementId);
  java.util.List<Signal> findByStartDateBefore(java.time.LocalDate date);
}
