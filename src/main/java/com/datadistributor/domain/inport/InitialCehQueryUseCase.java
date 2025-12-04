package com.datadistributor.domain.inport;

import java.util.Optional;

/**
 * Use case to query whether a signal already has an initial CEH event id.
 */
public interface InitialCehQueryUseCase {

  Optional<String> findInitialCehId(Long signalId);
}
