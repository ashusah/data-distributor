package com.datadistributor.inadapter.rest;

import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.JobResult;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/signal-events")
@Slf4j
public class SignalEventController {

  private final SignalEventProcessingUseCase processingUseCase;
  private final ThreadPoolTaskExecutor taskExecutor;

  public SignalEventController(SignalEventProcessingUseCase processingUseCase,
                               @Qualifier("dataDistributorTaskExecutor") ThreadPoolTaskExecutor taskExecutor) {
    this.processingUseCase = processingUseCase;
    this.taskExecutor = taskExecutor;
  }

  @GetMapping("/process-async")
  public ResponseEntity<JobResult> processAsync(@RequestParam("date") @NotNull LocalDate date) {
    String jobId = UUID.randomUUID().toString();
    taskExecutor.submit(() -> {
      JobResult result = processingUseCase.processEventsForDate(jobId, date);
      log.info("Async job {} finished for {}: success={} failure={} message={}",
          jobId, date, result.getSuccessCount(), result.getFailureCount(), result.getMessage());
    });
    return ResponseEntity.accepted()
        .body(new JobResult(0, 0, "Job accepted with id " + jobId));
  }
}
