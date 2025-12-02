package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class SignalAuditQueryRepositoryAdapter implements SignalAuditQueryPort {

  private final SignalAuditRepository signalAuditRepository;

  @Override
  public boolean isEventSuccessful(Long uabsEventId, long consumerId) {
    if (uabsEventId == null) {
      return false;
    }
    return signalAuditRepository
        .findTopByUabsEventIdAndConsumerIdOrderByAuditIdDesc(uabsEventId, consumerId)
        .map(SignalAuditJpaEntity::getStatus)
        .map(this::isSuccessStatus)
        .orElse(false);
  }

  private boolean isSuccessStatus(String status) {
    if (status == null) return false;
    String normalized = status.trim().toUpperCase();
    return "PASS".equals(normalized) || "SUCCESS".equals(normalized);
  }
}
