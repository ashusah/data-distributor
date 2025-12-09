package com.datadistributor.inadapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.JobResult;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Unit tests for the REST controller driving async processing and job status lookup.
 */
class SignalEventControllerTest {

  @Mock
  private SignalEventProcessingUseCase processingUseCase;

  private ThreadPoolTaskExecutor executor;
  private JobStatusStore store;
  private SignalEventController controller;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    executor = new ThreadPoolTaskExecutor() {
      @Override
      public java.util.concurrent.Future<?> submit(Runnable task) {
        task.run();
        return CompletableFuture.completedFuture(null);
      }
    };
    executor.afterPropertiesSet();

    store = new JobStatusStore();
    controller = new SignalEventController(processingUseCase, executor, store);
  }

  @Test
  void processAsyncReturnsAcceptedAndRunsJob() throws Exception {
    LocalDate date = LocalDate.of(2024, 12, 3);
    when(processingUseCase.processEventsForDate(any(), eq(date)))
        .thenReturn(new JobResult(1, 0, 0, "done"));

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    ResponseEntity<JobResult> response = controller.processAsync(date);

    assertThat(response.getStatusCode().value()).isEqualTo(202);
    assertThat(response.getHeaders().getLocation()).isNotNull();
    String jobId = response.getBody().getMessage().replace("Job accepted with id ", "");

    assertThat(store.find(jobId))
        .get()
        .extracting(JobResult::getSuccessCount, JobResult::getFailureCount)
        .containsExactly(1, 0);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void jobStatusReturnsNotFoundWhenMissing() {
    assertThat(controller.jobStatus("missing").getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void jobStatusReturnsStoredResult() {
    JobResult result = new JobResult(2, 1, 0, "ok");
    store.record("job-123", result);

    ResponseEntity<JobResult> response = controller.jobStatus("job-123");
    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isSameAs(result);
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void processAsync_handlesNullDate() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> controller.processAsync(null))
        .isInstanceOf(Exception.class);

    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void processAsync_createsUniqueJobId() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    when(processingUseCase.processEventsForDate(any(), eq(date)))
        .thenReturn(new JobResult(1, 0, 0, "done"));

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    ResponseEntity<JobResult> response1 = controller.processAsync(date);
    ResponseEntity<JobResult> response2 = controller.processAsync(date);

    String jobId1 = response1.getBody().getMessage().replace("Job accepted with id ", "");
    String jobId2 = response2.getBody().getMessage().replace("Job accepted with id ", "");
    assertThat(jobId1).isNotEqualTo(jobId2);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void processAsync_setsLocationHeader() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    when(processingUseCase.processEventsForDate(any(), eq(date)))
        .thenReturn(new JobResult(1, 0, 0, "done"));

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    ResponseEntity<JobResult> response = controller.processAsync(date);

    assertThat(response.getHeaders().getLocation()).isNotNull();
    assertThat(response.getHeaders().getLocation().getPath()).contains("/jobs/");
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void processAsync_returnsAcceptedStatus() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    when(processingUseCase.processEventsForDate(any(), eq(date)))
        .thenReturn(new JobResult(1, 0, 0, "done"));

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    ResponseEntity<JobResult> response = controller.processAsync(date);

    assertThat(response.getStatusCode().value()).isEqualTo(202);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void processAsync_storesInitialAcceptedResult() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    when(processingUseCase.processEventsForDate(any(), eq(date)))
        .thenReturn(new JobResult(1, 0, 0, "done"));

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    ResponseEntity<JobResult> response = controller.processAsync(date);

    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).contains("Job accepted");
    String jobId = response.getBody().getMessage().replace("Job accepted with id ", "");
    // After async execution, the stored result will be updated
    Optional<JobResult> stored = store.find(jobId);
    assertThat(stored).isPresent();
    assertThat(stored.get().getSuccessCount()).isEqualTo(1);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void processAsync_updatesResultAfterProcessing() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    JobResult finalResult = new JobResult(5, 2, 7, "Complete");
    when(processingUseCase.processEventsForDate(any(), eq(date)))
        .thenReturn(finalResult);

    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    ResponseEntity<JobResult> response = controller.processAsync(date);

    String jobId = response.getBody().getMessage().replace("Job accepted with id ", "");
    // Wait a bit for async processing
    try {
      Thread.sleep(100);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    Optional<JobResult> stored = store.find(jobId);
    assertThat(stored).isPresent();
    assertThat(stored.get().getSuccessCount()).isEqualTo(5);
    assertThat(stored.get().getFailureCount()).isEqualTo(2);
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void jobStatus_returnsNotFoundForUnknownJob() {
    ResponseEntity<JobResult> response = controller.jobStatus("unknown-job-id");

    assertThat(response.getStatusCode().value()).isEqualTo(404);
    assertThat(response.getBody()).isNull();
  }

  @Test
  void jobStatus_returnsStoredResultForKnownJob() {
    JobResult result = new JobResult(10, 5, 15, "Done");
    store.record("known-job", result);

    ResponseEntity<JobResult> response = controller.jobStatus("known-job");

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isSameAs(result);
  }

  @Test
  void jobStatus_handlesEmptyJobId() {
    ResponseEntity<JobResult> response = controller.jobStatus("");

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }

  @Test
  void jobStatus_handlesNullJobId() {
    ResponseEntity<JobResult> response = controller.jobStatus(null);

    assertThat(response.getStatusCode().value()).isEqualTo(404);
  }
}
