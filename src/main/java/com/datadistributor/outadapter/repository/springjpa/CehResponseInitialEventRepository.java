package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.CehResponseInitialEventEntity;
import com.datadistributor.outadapter.entity.CehResponseInitialEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for CEH initial response mappings keyed by composite id.
 */
@Repository
public interface CehResponseInitialEventRepository extends JpaRepository<CehResponseInitialEventEntity, CehResponseInitialEventId> {

  java.util.Optional<CehResponseInitialEventEntity> findFirstByIdSignalId(Long signalId);
}
