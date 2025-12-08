package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.CehResponseInitialEventEntity;
import com.datadistributor.outadapter.entity.CehResponseInitialEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for CEH initial response mappings keyed by composite id.
 */
@Repository
public interface CehResponseInitialEventRepository extends JpaRepository<CehResponseInitialEventEntity, CehResponseInitialEventId> {

  @Query("select e from CehResponseInitialEventEntity e where e.id.signalId = :signalId order by e.id.cehInitialEventId asc")
  java.util.List<CehResponseInitialEventEntity> findByIdSignalId(@Param("signalId") Long signalId);
  
  default java.util.Optional<CehResponseInitialEventEntity> findFirstByIdSignalId(Long signalId) {
    java.util.List<CehResponseInitialEventEntity> results = findByIdSignalId(signalId);
    return results.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(results.get(0));
  }
}
