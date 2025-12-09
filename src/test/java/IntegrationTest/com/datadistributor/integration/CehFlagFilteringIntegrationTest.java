package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalEventJpaRepository;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test to verify that findPageForCEH correctly filters events based on
 * reportCW014ToCEH flag in ProductRiskMonitoringJpaEntity.
 */
class CehFlagFilteringIntegrationTest extends AbstractIntegrationTest {

  @Autowired
  private SignalEventUseCase signalEventUseCase;
  @Autowired
  private SignalEventJpaRepository eventRepo;
  @Autowired
  private SignalJpaRepository signalRepo;
  @Autowired
  private AccountBalanceJpaRepository accountRepo;
  @Autowired
  private EntityManager entityManager;

  private final LocalDate testDate = LocalDate.of(2025, 1, 15);

  @BeforeEach
  void setup() {
    eventRepo.deleteAll();
    signalRepo.deleteAll();
  }

  @Test
  void findPageForCEH_onlyReturnsEventsWithReportFlagSetToY() {
    // Create signal - ID will be auto-generated
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setAgreementId(200L);
    signal.setSignalStartDate(testDate);
    signal = signalRepo.save(signal);
    long signalId = signal.getSignalId();

    // Create event with reportCW014ToCEH = 'Y' (should be included)
    SignalEventJpaEntity reportableEvent = createEvent(signalId, 200L, testDate.atTime(10, 0), 300L, "Y");

    // Create event with reportCW014ToCEH = 'N' (should be excluded)
    SignalEventJpaEntity nonReportableEvent = createEvent(signalId, 200L, testDate.atTime(11, 0), 300L, "N");

    // Create event with reportCW014ToCEH = 'Y' but low balance (should be excluded)
    SignalEventJpaEntity lowBalanceEvent = createEvent(signalId, 200L, testDate.atTime(12, 0), 100L, "Y");

    // Query for CEH events
    var result = signalEventUseCase.getAllSignalForCEH(testDate);

    // Should only return the event with reportCW014ToCEH = 'Y' and balance >= 250
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getUabsEventId()).isEqualTo(reportableEvent.getUabsEventId());
  }

  @Test
  void findPageForCEH_excludesEventsWithReportFlagSetToN() {
    // Create signal - ID will be auto-generated
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setAgreementId(201L);
    signal.setSignalStartDate(testDate);
    signal = signalRepo.save(signal);
    long signalId = signal.getSignalId();

    // Create multiple events with reportCW014ToCEH = 'N'
    for (int i = 0; i < 3; i++) {
      createEvent(signalId, 201L, testDate.atTime(10 + i, 0), 500L, "N");
    }

    // Query for CEH events
    var result = signalEventUseCase.getAllSignalForCEH(testDate);

    // Should return empty list since all events have reportCW014ToCEH = 'N'
    assertThat(result).isEmpty();
  }

  @Test
  void findPageForCEH_includesOnlyEventsWithReportFlagSetToY() {
    // Create signal - ID will be auto-generated
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setAgreementId(202L);
    signal.setSignalStartDate(testDate);
    signal = signalRepo.save(signal);
    long signalId = signal.getSignalId();

    // Create mix of reportable and non-reportable events
    SignalEventJpaEntity reportable1 = createEvent(signalId, 202L, testDate.atTime(10, 0), 300L, "Y");
    SignalEventJpaEntity nonReportable = createEvent(signalId, 202L, testDate.atTime(11, 0), 300L, "N");
    SignalEventJpaEntity reportable2 = createEvent(signalId, 202L, testDate.atTime(12, 0), 400L, "Y");

    // Query for CEH events
    var result = signalEventUseCase.getAllSignalForCEH(testDate);

    // Should only return events with reportCW014ToCEH = 'Y'
    assertThat(result).hasSize(2);
    assertThat(result)
        .extracting(SignalEvent::getUabsEventId)
        .containsExactlyInAnyOrder(reportable1.getUabsEventId(), reportable2.getUabsEventId());
  }

  SignalEventJpaEntity createEvent(long signalId, long agreementId, LocalDateTime timestamp,
                                           long balance, String reportToCEH) {
    SignalJpaEntity signal = signalRepo.findById(signalId).orElseThrow();
    
    // Create ProductRiskMonitoringJpaEntity with specified report flag
    // Use different GRV values to avoid conflicts: GRV 1 for 'Y', GRV 2 for 'N'
    short grv = reportToCEH.equals("Y") ? (short) 1 : (short) 2;
    ProductRiskMonitoringJpaEntity prm = ensureProductRiskMonitoring(grv);
    prm.setReportCW014ToCEH(reportToCEH);
    
    // Ensure account exists with the correct GRV - use account's MERGE cascade to persist ProductRiskMonitoringJpaEntity
    AccountBalanceJpaEntity account = accountRepo.findById(agreementId).orElseGet(() -> {
      AccountBalanceJpaEntity newAccount = new AccountBalanceJpaEntity();
      newAccount.setAgreementId(agreementId);
      newAccount.setGrv(prm);
      newAccount.setIban("DE1234567890123456");
      newAccount.setLifeCycleStatus((short) 1);
      newAccount.setBcNumber(agreementId);
      newAccount.setCurrencyCode("EUR");
      newAccount.setBookDate(LocalDate.now());
      newAccount.setUnauthorizedDebitBalance(0L);
      newAccount.setLastBookDateBalanceCrToDt(LocalDate.now());
      newAccount.setIsAgreementPartOfAcbs("Y");
      return accountRepo.save(newAccount); // MERGE cascade will persist ProductRiskMonitoringJpaEntity
    });
    
    // Update account's GRV if it has a different one or different flag
    if (account.getGrv() == null || !account.getGrv().getGrv().equals(grv) || !reportToCEH.equals(account.getGrv().getReportCW014ToCEH())) {
      account.setGrv(prm);
      account = accountRepo.save(account); // MERGE cascade will update ProductRiskMonitoringJpaEntity
    }
    
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setSignal(signal);
    entity.setAgreementId(agreementId);
    entity.setEventRecordDateTime(timestamp);
    entity.setEventType("OVERLIMIT_SIGNAL");
    entity.setEventStatus("OVERLIMIT_SIGNAL");
    entity.setUnauthorizedDebitBalance(balance);
    entity.setBookDate(timestamp.toLocalDate());
    entity.setGrv(prm);
    entity.setProductId((short) 1);

    // Save through repository (transactional) - MERGE cascade will handle ProductRiskMonitoringJpaEntity
    return eventRepo.save(entity);
  }
}

