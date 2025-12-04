package com.datadistributor.support;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles database setup for integration tests so test classes stay focused on behavior.
 */
public class TestSignalDataSeeder {

  private final SignalEventJpaRepository signalRepo;
  private final SignalAuditRepository auditRepo;

  public TestSignalDataSeeder(SignalEventJpaRepository signalRepo, SignalAuditRepository auditRepo) {
    this.signalRepo = signalRepo;
    this.auditRepo = auditRepo;
  }

  public void resetData() {
    auditRepo.deleteAll();
    signalRepo.deleteAll();
  }

  public List<Long> seedSignalEvents(LocalDate date, int count) {
    List<Long> ids = new ArrayList<>();
    for (long i = 1; i <= count; i++) {
      SignalEventJpaEntity entity = new SignalEventJpaEntity();
      entity.setSignalId(2000L + i);
      entity.setAgreementId(3000L + i);
      entity.setEventRecordDateTime(LocalDateTime.of(date, LocalTime.of(1, 0)));
      entity.setEventType("CONTRACT_UPDATE");
      entity.setEventStatus("OVERLIMIT_SIGNAL");
      entity.setUnauthorizedDebitBalance(300L);
      entity.setBookDate(date.minusDays(5));
      entity.setGrv((short) 1);
      entity.setProductId((short) 1);
      SignalEventJpaEntity saved = signalRepo.save(entity);
      ids.add(saved.getUabsEventId());
    }
    return ids;
  }
}
