package com.datadistributor.inadapter.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.JobResult;
import java.time.LocalDate;
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
}
