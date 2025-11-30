package com.datadistributor.inadapter.scheduler;

import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.JobResult;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter: time-based driver that triggers the SignalEvent processing use case.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SignalEventScheduler {

  private final SignalEventProcessingUseCase processingUseCase;

  /**
   * Daily 02:00 run for Tue–Sat using today's date. Skip Sunday and Monday.
   */
  @Scheduled(cron = "0 0 2 * * *")
  public void runDaily2am() {
    LocalDate today = LocalDate.now();
    DayOfWeek dow = today.getDayOfWeek();
    if (dow == DayOfWeek.SUNDAY || dow == DayOfWeek.MONDAY) {
      log.info("2am schedule skipped for {} ({})", today, dow);
      return;
    }
    trigger("sched-2am", today);
  }

  /**
   * Monday 10:00 run using yesterday's date (Sunday).
   */
  @Scheduled(cron = "0 0 10 * * MON")
  public void runMonday10am() {
    LocalDate targetDate = LocalDate.now().minusDays(1); // Sunday
    trigger("sched-mon-10", targetDate);
  }

  /**
   * Monday 12:00 run using today's date (Monday).
   */
  @Scheduled(cron = "0 0 12 * * MON")
  public void runMondayNoon() {
    LocalDate targetDate = LocalDate.now(); // Monday
    trigger("sched-mon-12", targetDate);
  }

  private void trigger(String prefix, LocalDate date) {
    String jobId = prefix + "-" + UUID.randomUUID();
    log.info("Scheduler kicking off jobId={} for date={}", jobId, date);
    JobResult result = processingUseCase.processEventsForDate(jobId, date);
    log.info("Scheduler jobId={} for date={} finished. success={} failure={} message={}",
        jobId, date, result.getSuccessCount(), result.getFailureCount(), result.getMessage());
  }
}
