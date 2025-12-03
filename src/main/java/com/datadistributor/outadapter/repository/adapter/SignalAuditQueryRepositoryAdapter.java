package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Provides audit lookup helpers: checks last status for an event and lists failed event ids for a
 * given date (latest audit per event). Normalizes PASS/SUCCESS status strings.
 */
@Repository
@RequiredArgsConstructor
public class SignalAuditQueryRepositoryAdapter implements SignalAuditQueryPort {

  private final SignalAuditRepository signalAuditRepository;
  private final DataDistributorProperties properties;

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

  @Override
  public List<Long> findFailedEventIdsForDate(LocalDate date) {
    if (date == null) {
      return List.of();
    }
    LocalDateTime start = date.atStartOfDay();
    LocalDateTime end = date.atTime(LocalTime.MAX);
    long consumerId = properties.getAudit().getConsumerId();

    List<SignalAuditJpaEntity> audits = signalAuditRepository
        .findByAuditRecordDateTimeBetweenAndConsumerIdOrderByAuditRecordDateTimeDesc(start, end, consumerId);

    Map<Long, SignalAuditJpaEntity> latestByEvent = new LinkedHashMap<>();
    for (SignalAuditJpaEntity audit : audits) {
      if (audit == null || audit.getUabsEventId() == null) {
        continue;
      }
      latestByEvent.putIfAbsent(audit.getUabsEventId(), audit);
    }

    return latestByEvent.values().stream()
        .filter(Objects::nonNull)
        .filter(audit -> !isSuccessStatus(audit.getStatus()))
        .map(SignalAuditJpaEntity::getUabsEventId)
        .toList();
  }

  private boolean isSuccessStatus(String status) {
    if (status == null) return false;
    String normalized = status.trim().toUpperCase();
    return "PASS".equals(normalized) || "SUCCESS".equals(normalized);
  }
}
