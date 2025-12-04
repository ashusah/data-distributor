package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.AccountBalance;
import com.datadistributor.domain.Signal;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.inport.SignalUseCase;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.domain.outport.FileStoragePort;
import com.datadistributor.domain.service.DialSignalDataExportDomainService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DialSignalDataExportServiceTest {

  private final CapturingStorage storage = new CapturingStorage();
  private final List<SignalEvent> events = new ArrayList<>();
  private DialSignalDataExportDomainService service;

  @BeforeEach
  void setUp() {
    DataDistributorProperties props = new DataDistributorProperties();
    props.getStorage().setDialFolder("dial-folder");
    props.getStorage().setDialFilePrefix("dial-prefix");

    service = new DialSignalDataExportDomainService(
        new StubSignalEventUseCase(),
        new StubSignalQueryUseCase(),
        new StubAccountPort(),
        storage,
        props);
  }

  @Test
  void exportsAllEventsForDateToStorage() {
    SignalEvent e1 = new SignalEvent();
    e1.setUabsEventId(1L);
    e1.setSignalId(11L);
    e1.setAgreementId(210L);
    events.add(e1);

    service.export(LocalDate.of(2025, 12, 2));

    assertThat(storage.folder).isEqualTo("dial-folder");
    assertThat(storage.fileName).isEqualTo("dial-prefix-2025-12-02.csv");
    assertThat(storage.content).contains("EventId");
    assertThat(storage.content).contains("999");
    assertThat(storage.content).contains("\n1,");
  }

  private static class CapturingStorage implements FileStoragePort {
    String folder;
    String fileName;
    String content;

    @Override
    public void upload(String folder, String fileName, String content) {
      this.folder = folder;
      this.fileName = fileName;
      this.content = content;
    }
  }

  private class StubSignalEventUseCase implements SignalEventUseCase {
    @Override
    public List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) {
      return events;
    }

    @Override
    public List<SignalEvent> getAllSignalForCEH(LocalDate date) {
      return events;
    }
  }

  private static class StubSignalQueryUseCase implements SignalUseCase {
    @Override
    public java.util.Optional<Signal> findBySignalId(Long signalId) {
      Signal s = new Signal();
      s.setSignalId(signalId);
      s.setAgreementId(999L);
      s.setSignalStartDate(LocalDate.of(2025, 12, 1));
      s.setSignalEndDate(LocalDate.of(2025, 12, 3));
      return java.util.Optional.of(s);
    }

    @Override
    public java.util.Optional<Signal> getOpenSignalOfAgreement(Long agreementId) {
      if (agreementId == null) {
        return java.util.Optional.empty();
      }
      Signal s = new Signal();
      s.setSignalId(agreementId + 1);
      s.setAgreementId(999L);
      s.setSignalStartDate(LocalDate.of(2025, 12, 1));
      s.setSignalEndDate(LocalDate.of(2025, 12, 3));
      return java.util.Optional.of(s);
    }
  }

  private static class StubAccountPort implements AccountBalanceOverviewPort {
    @Override
    public java.util.Optional<Long> findBcNumberByAgreementId(Long agreementId) {
      return java.util.Optional.of(321L);
    }

    @Override
    public java.util.Optional<AccountBalance> findByAgreementId(Long agreementId) {
      AccountBalance abo = new AccountBalance();
      abo.setAgreementId(agreementId);
      abo.setIban("DE1234567890123456");
      abo.setCurrencyCode("EUR");
      abo.setBcNumber(321L);
      return java.util.Optional.of(abo);
    }
  }
}
