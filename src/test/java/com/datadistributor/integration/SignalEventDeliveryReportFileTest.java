package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.job.JobProgressTracker;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.outport.SignalAuditQueryPort;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventRepository;
import com.datadistributor.domain.inport.SignalDispatchSelectorUseCase;
import com.datadistributor.domain.service.SignalEventProcessingService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SignalEventDeliveryReportFileTest {

  @TempDir
  Path tempDir;

  @Test
  void createsDeliveryReportFileWithCounts() throws Exception {
    var events = List.of(sampleEvent(1L), sampleEvent(2L), sampleEvent(3L));
    var repository = new StubSignalEventRepository(events, events.size());
    var batchPort = new StubBatchPort(new BatchResult(2, 1));
    var publisher = new FileWritingReportPublisher(tempDir);

    var service = new SignalEventProcessingService(
        repository,
        batchPort,
        new AlwaysPassAuditQueryPort(),
        new StubDispatchSelector(repository),
        5,
        new JobProgressTracker(),
        publisher);

    JobResult result = service.processEventsForDate("job-1", LocalDate.of(2025, 12, 2));

    assertThat(result.getSuccessCount()).isEqualTo(2);
    assertThat(result.getFailureCount()).isEqualTo(1);
    assertThat(result.getTotalCount()).isEqualTo(3);

    Path reportFile = publisher.writtenFile;
    assertThat(reportFile).exists();
    String content = Files.readString(reportFile);
    String expected = Files.readString(Path.of("src/test/resources/fixtures/ceh-report-expected.txt")).trim();
    assertThat(content.trim()).isEqualTo(expected);
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

    @Override
    public Optional<SignalEvent> getEarliestOverlimitEvent(Long signalId) {
      return events.stream().findFirst();
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

  private static class StubDispatchSelector implements SignalDispatchSelectorUseCase {
    private final SignalEventRepository repo;
    StubDispatchSelector(SignalEventRepository repo) { this.repo = repo; }
    @Override
    public List<SignalEvent> selectEventsToSend(LocalDate targetDate) {
      return repo.getSignalEventsForCEH(targetDate, 0, 100);
    }
  }

  private static class FileWritingReportPublisher implements DeliveryReportPublisher {
    private final Path baseDir;
    Path writtenFile;

    FileWritingReportPublisher(Path baseDir) {
      this.baseDir = baseDir;
    }

    @Override
    public void publish(com.datadistributor.domain.report.DeliveryReport report) {
      try {
        Path file = baseDir.resolve("ceh-report-" + report.getDate() + ".txt");
        Files.writeString(file, report.getContent());
        this.writtenFile = file;
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }
}
