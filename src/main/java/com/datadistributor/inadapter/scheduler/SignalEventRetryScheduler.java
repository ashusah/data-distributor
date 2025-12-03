package com.datadistributor.inadapter.scheduler;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.inport.SignalEventRetryUseCase;
import com.datadistributor.domain.job.JobResult;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Hourly retry driver (13:00–23:00 except Monday). When enabled, invokes the retry use case for
 * the current date to re-send failed events. Skips if disabled or if day is Monday.
 */
@Component
@Slf4j
public class SignalEventRetryScheduler {

  private final SignalEventRetryUseCase retryUseCase;
  private final DataDistributorProperties properties;
  private final Clock clock;

  public SignalEventRetryScheduler(SignalEventRetryUseCase retryUseCase,
                                   DataDistributorProperties properties,
                                   Clock clock) {
    this.retryUseCase = retryUseCase;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(cron = "${data-distributor.scheduler.retry-cron:0 0 13-23 * * TUE,WED,THU,FRI,SAT,SUN}")
  public void retryHourly() {
    if (!properties.getScheduler().isEnableRetry()) {
      log.info("Retry schedule disabled via config");
      return;
    }
    LocalDate today = LocalDate.now(clock);
    if (today.getDayOfWeek() == DayOfWeek.MONDAY) {
      log.info("Retry schedule skipped for Monday {}", today);
      return;
    }
    trigger(today);
  }

  private void trigger(LocalDate date) {
    String jobId = "retry-" + UUID.randomUUID();
    log.info("Retry scheduler kicking off jobId={} for date={}", jobId, date);
    JobResult result = retryUseCase.retryFailedEvents(jobId, date);
    log.info("Retry jobId={} for date={} finished. success={} failure={} message={}",
        jobId, date, result.getSuccessCount(), result.getFailureCount(), result.getMessage());
  }
}
