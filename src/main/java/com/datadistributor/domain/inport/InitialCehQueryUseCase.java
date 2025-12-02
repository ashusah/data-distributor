package com.datadistributor.domain.inport;

import java.util.Optional;

public interface InitialCehQueryUseCase {

  Optional<String> findInitialCehId(Long signalId);
}
