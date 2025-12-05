package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.outadapter.entity.CehResponseInitialEventEntity;
import com.datadistributor.outadapter.entity.CehResponseInitialEventId;
import com.datadistributor.outadapter.repository.springjpa.CehResponseInitialEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Adapter that persists and fetches CEH initial event mappings on behalf of the domain while
 * shielding core logic from JPA specifics.
 */
@Repository
@RequiredArgsConstructor
public class InitialCehMappingRepositoryAdapter implements InitialCehMappingPort {

  private final CehResponseInitialEventRepository cehResponseInitialEventRepository;

  @Override
  public void saveInitialCehMapping(Long signalId, long cehId) {
    CehResponseInitialEventId id = new CehResponseInitialEventId(
        String.valueOf(cehId),
        signalId);
    CehResponseInitialEventEntity initial = new CehResponseInitialEventEntity(id);
    cehResponseInitialEventRepository.save(initial);
  }

  @Override
  public Optional<String> findInitialCehId(Long signalId) {
    return cehResponseInitialEventRepository
        .findFirstByIdSignalId(signalId)
        .map(event -> event.getId().getCehInitialEventId());
  }
}
