package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.service.DialSignalDataExportService;
import com.datadistributor.domain.service.SignalEventProcessingService;
import com.datadistributor.domain.outport.FileStoragePort;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class DeliveryReportIntegrationTest {

  @Test
  void generatesCehDeliveryReportWithCounts() {
    var events = List.of(sampleEvent(1L), sampleEvent(2L), sampleEvent(3L));
    var repository = new StubSignalEventRepository(events, 3);
    var batchPort = new StubBatchPort(new BatchResult(2, 1));
    var publisher = new CapturingReportPublisher();
    var service = new SignalEventProcessingService(
        repository,
        batchPort,
        new AlwaysPassAuditQueryPort(),
        5,
        new JobProgressTracker(),
        publisher);

    JobResult result = service.processEventsForDate("job-1", LocalDate.of(2025, 12, 2));

    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getTotalCount()).isEqualTo(3);
    assertThat(publisher.content)
        .contains("UABS DELIVERY TO CEH REPORT")
        .contains("Total number of events for Date 2025-12-02 = 3")
        .contains("PASS status- 2")
        .contains("not sent to CEH (with FAIL status)- 1");
  }

  @Test
  void createsDialExportCsvWithEvents() {
    var props = new DataDistributorProperties();
    props.getStorage().setDialFolder("dial-folder");
    props.getStorage().setDialFilePrefix("dial-prefix");
    var events = new ArrayList<SignalEvent>();
    events.add(sampleEvent(10L));
    var storage = new CapturingStorage();
    var service = new DialSignalDataExportService(new StubSignalEventUseCase(events), storage, props);

    service.export(LocalDate.of(2025, 12, 2));

    assertThat(storage.folder).isEqualTo("dial-folder");
    assertThat(storage.fileName).isEqualTo("dial-prefix-2025-12-02.csv");
    assertThat(storage.content).contains("uabs_event_id");
    assertThat(storage.content).contains("10");
  }

  private SignalEvent sampleEvent(Long id) {
    SignalEvent e = new SignalEvent();
    e.setUabsEventId(id);
    e.setSignalId(100L + id);
    e.setAgreementId(200L + id);
    e.setEventRecordDateTime(LocalDateTime.now());
    e.setEventType("TYPE");
    e.setEventStatus("STATUS");
    return e;
  }

  private static class StubSignalEventRepository implements SignalEventRepository {
    private final List<SignalEvent> events;
    private final long totalCount;

    StubSignalEventRepository(List<SignalEvent> events, long totalCount) {
      this.events = events;
      this.totalCount = totalCount;
    }

    @Override
    public List<SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) {
      return Collections.emptyList();
    }

    @Override
    public List<SignalEvent> getSignalEventsForCEH(LocalDate date, int page, int size) {
      return page == 0 ? events : Collections.emptyList();
    }

    @Override
    public long countSignalEventsForCEH(LocalDate date) {
      return totalCount;
    }

    @Override
    public Optional<SignalEvent> getPreviousEvent(Long signalId, LocalDateTime before) {
      return Optional.empty();
    }
  }

  private static class StubBatchPort implements SignalEventBatchPort {
    private final BatchResult result;

    StubBatchPort(BatchResult result) {
      this.result = result;
    }

    @Override
    public CompletableFuture<BatchResult> submitBatch(List<SignalEvent> events) {
      return CompletableFuture.completedFuture(result);
    }
  }

  private static class AlwaysPassAuditQueryPort implements SignalAuditQueryPort {
    @Override
    public boolean isEventSuccessful(Long uabsEventId, long consumerId) {
      return true;
    }
  }

  private static class CapturingReportPublisher implements DeliveryReportPublisher {
    String content;

    @Override
    public void publish(com.datadistributor.domain.report.DeliveryReport report) {
      this.content = report.getContent();
    }
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

  private static class StubSignalEventUseCase implements com.datadistributor.domain.inport.SignalEventUseCase {
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
}
