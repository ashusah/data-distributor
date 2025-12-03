package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.outport.FileStoragePort;
import com.datadistributor.domain.service.DialSignalDataExportService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DialSignalDataExportServiceFileTest {

  @TempDir
  Path tempDir;

  @Test
  void writesDialExportCsvToStorageFolder() throws Exception {
    var props = new DataDistributorProperties();
    props.getStorage().setDialFolder(tempDir.resolve("dial").toString());
    props.getStorage().setDialFilePrefix("dial-prefix");

    var events = List.of(sampleEvent(10L));
    var service = new DialSignalDataExportService(
        new StubSignalEventUseCase(events),
        new FileWritingStorage(),
        props);

    service.export(LocalDate.of(2025, 12, 2));

    Path expected = tempDir.resolve("dial").resolve("dial-prefix-2025-12-02.csv");
    assertThat(expected).exists();
    String content = Files.readString(expected).trim();
    String fixture = Files.readString(Path.of("src/test/resources/fixtures/dial-export-expected.csv")).trim();
    assertThat(content).isEqualTo(fixture);
  }

  private SignalEvent sampleEvent(Long id) {
    SignalEvent e = new SignalEvent();
    e.setUabsEventId(id);
    e.setSignalId(100L + id);
    e.setAgreementId(200L + id);
    e.setEventRecordDateTime(LocalDateTime.of(2025, 12, 2, 10, 15, 30));
    e.setEventType("TYPE");
    e.setEventStatus("STATUS");
    e.setUnauthorizedDebitBalance(500L);
    e.setBookDate(LocalDate.of(2025, 12, 1));
    e.setGrv((short) 1);
    e.setProductId((short) 2);
    return e;
  }

  private static class StubSignalEventUseCase implements SignalEventUseCase {
    private final List<SignalEvent> events;

    StubSignalEventUseCase(List<SignalEvent> events) {
      this.events = events;
    }

    @Override
    public List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) {
      return events;
    }

    @Override
    public List<SignalEvent> getAllSignalForCEH(LocalDate date) {
      return events;
    }
  }

  private class FileWritingStorage implements FileStoragePort {
    @Override
    public void upload(String folder, String fileName, String content) {
      try {
        Path dir = Path.of(folder);
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(fileName), content);
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
