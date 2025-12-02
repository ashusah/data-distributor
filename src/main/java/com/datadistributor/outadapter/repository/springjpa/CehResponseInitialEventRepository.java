package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.CehResponseInitialEvent;
import com.datadistributor.outadapter.entity.CehResponseInitialEventId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CehResponseInitialEventRepository extends JpaRepository<CehResponseInitialEvent, CehResponseInitialEventId> {

  java.util.Optional<CehResponseInitialEvent> findFirstByIdSignalId(Long signalId);
}
