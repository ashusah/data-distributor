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
    // Delete all existing mappings for this signalId to prevent duplicates
    // (there should only be one mapping per signalId, but the composite key allows multiple)
    java.util.List<CehResponseInitialEventEntity> existing = cehResponseInitialEventRepository
        .findByIdSignalId(signalId);
    
    if (!existing.isEmpty()) {
      // Check if any existing mapping has the same cehId
      boolean hasSameCehId = existing.stream()
          .anyMatch(e -> String.valueOf(cehId).equals(e.getId().getCehInitialEventId()));
      
      if (!hasSameCehId) {
        // Delete all existing and create new with the new cehId
        cehResponseInitialEventRepository.deleteAll(existing);
        CehResponseInitialEventId newId = new CehResponseInitialEventId(
            String.valueOf(cehId),
            signalId);
        CehResponseInitialEventEntity initial = new CehResponseInitialEventEntity(newId);
        cehResponseInitialEventRepository.save(initial);
      }
      // If same cehId already exists, do nothing
    } else {
      // Create new mapping
      CehResponseInitialEventId id = new CehResponseInitialEventId(
          String.valueOf(cehId),
          signalId);
      CehResponseInitialEventEntity initial = new CehResponseInitialEventEntity(id);
      cehResponseInitialEventRepository.save(initial);
    }
  }

  @Override
  public Optional<String> findInitialCehId(Long signalId) {
    return cehResponseInitialEventRepository
        .findFirstByIdSignalId(signalId)
        .map(event -> event.getId().getCehInitialEventId());
  }
}
