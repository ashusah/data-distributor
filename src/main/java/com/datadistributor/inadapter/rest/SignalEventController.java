package com.datadistributor.inadapter.rest;

import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.JobResult;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/signal-events")
@Slf4j
public class SignalEventController {

  private final SignalEventProcessingUseCase processingUseCase;
  private final ThreadPoolTaskExecutor taskExecutor;
  private final JobStatusStore jobStatusStore;

  public SignalEventController(SignalEventProcessingUseCase processingUseCase,
                               @Qualifier("dataDistributorTaskExecutor") ThreadPoolTaskExecutor taskExecutor,
                               JobStatusStore jobStatusStore) {
    this.processingUseCase = processingUseCase;
    this.taskExecutor = taskExecutor;
    this.jobStatusStore = jobStatusStore;
  }

  @PostMapping("/process-async")
  public ResponseEntity<JobResult> processAsync(@RequestParam("date") @NotNull LocalDate date) {
    String jobId = UUID.randomUUID().toString();
    JobResult accepted = new JobResult(0, 0, "Job accepted with id " + jobId);
    jobStatusStore.record(jobId, accepted);

    taskExecutor.submit(() -> {
      JobResult result = processingUseCase.processEventsForDate(jobId, date);
      jobStatusStore.record(jobId, result);
      log.info("Async job {} finished for {}: success={} failure={} message={}",
          jobId, date, result.getSuccessCount(), result.getFailureCount(), result.getMessage());
    });

    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .path("/jobs/{jobId}")
        .buildAndExpand(jobId)
        .toUri();

    return ResponseEntity.accepted()
        .location(location)
        .body(accepted);
  }

  @GetMapping("/jobs/{jobId}")
  public ResponseEntity<JobResult> jobStatus(@PathVariable String jobId) {
    return jobStatusStore.find(jobId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
