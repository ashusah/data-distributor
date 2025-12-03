package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.DataDistributorApplication;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalAuditRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = DataDistributorApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class OverdueDispatchIntegrationTest {

  @Autowired
  private SignalEventProcessingUseCase processingUseCase;
  @Autowired
  private SignalEventJpaRepository eventRepo;
  @Autowired
  private SignalAuditRepository auditRepo;
  @Autowired
  private SignalJpaRepository signalRepo;

  private final LocalDate startDate = LocalDate.of(2025, 12, 1);

  @BeforeEach
  void setup() {
    auditRepo.deleteAll();
    eventRepo.deleteAll();
    signalRepo.deleteAll();
  }

  @Test
  void sendsEarliestOverlimitWhenDpdExceedsThresholdEvenWithoutTodayEvent() {
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setSignalId(900L);
    signal.setAgreementId(800L);
    signal.setSignalStartDate(startDate);
    signalRepo.save(signal);

    SignalEventJpaEntity event = new SignalEventJpaEntity();
    event.setSignalId(signal.getSignalId());
    event.setAgreementId(signal.getAgreementId());
    event.setEventRecordDateTime(LocalDateTime.of(startDate, LocalTime.of(8, 0)));
    event.setEventStatus("OVERLIMIT_SIGNAL");
    event.setEventType("OVERLIMIT_SIGNAL");
    event.setUnauthorizedDebitBalance(10L);
    event.setBookDate(startDate);
    eventRepo.save(event);

    LocalDate processingDate = startDate.plusDays(5); // DPD6, no events on this date
    processingUseCase.processEventsForDate("job-overdue", processingDate);

    assertThat(auditRepo.findAll()).hasSize(1);
    assertThat(auditRepo.findAll().get(0).getUabsEventId()).isEqualTo(event.getUabsEventId());
  }
}
