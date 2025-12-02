package com.datadistributor.outadapter.web;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.outadapter.entity.SignalAuditJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SignalAuditService {

  private final SignalAuditRepository signalAuditRepository;
  private final DataDistributorProperties properties;

  public void persistAudit(SignalEvent event, String status, String responseCode, String message) {
    SignalAuditJpaEntity audit = new SignalAuditJpaEntity();
    audit.setSignalId(event.getSignalId());
    audit.setUabsEventId(event.getUabsEventId());
    audit.setConsumerId(properties.getAudit().getConsumerId());
    audit.setAgreementId(event.getAgreementId());
    audit.setUnauthorizedDebitBalance(
        event.getUnauthorizedDebitBalance() == null ? 0L : event.getUnauthorizedDebitBalance());
    audit.setStatus(status);
    audit.setResponseCode(truncate(responseCode, 10));
    audit.setResponseMessage(truncate(message, 100));
    audit.setAuditRecordDateTime(LocalDateTime.now());
    signalAuditRepository.save(audit);
  }

  public void logAuditFailure(SignalEvent event, Exception ex) {
    log.error("LOG001- Audit Record uabsEventId={} failed to be persisted: {}",
        event == null ? "unknown" : event.getUabsEventId(),
        ex.toString(),
        ex);
  }

  private String truncate(String value, int max) {
    if (value == null) return "";
    if (value.length() <= max) return value;
    return value.substring(0, max);
  }
}
