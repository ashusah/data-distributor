package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.outport.FileStoragePort;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DialSignalDataExportServiceTest {

  private final CapturingStorage storage = new CapturingStorage();
  private final List<SignalEvent> events = new ArrayList<>();
  private DialSignalDataExportService service;

  @BeforeEach
  void setUp() {
    DataDistributorProperties props = new DataDistributorProperties();
    props.getStorage().setDialFolder("dial-folder");
    props.getStorage().setDialFilePrefix("dial-prefix");

    service = new DialSignalDataExportService(new StubSignalEventUseCase(), storage, props);
  }

  @Test
  void exportsAllEventsForDateToStorage() {
    SignalEvent e1 = new SignalEvent();
    e1.setUabsEventId(1L);
    e1.setSignalId(11L);
    events.add(e1);

    service.export(LocalDate.of(2025, 12, 2));

    assertThat(storage.folder).isEqualTo("dial-folder");
    assertThat(storage.fileName).isEqualTo("dial-prefix-2025-12-02.csv");
    assertThat(storage.content).contains("uabs_event_id");
    assertThat(storage.content).contains("1,11");
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
}
