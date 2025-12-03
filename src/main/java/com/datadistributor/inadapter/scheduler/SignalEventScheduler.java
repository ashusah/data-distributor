package com.datadistributor.inadapter.scheduler;

import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.JobResult;
import com.datadistributor.application.config.DataDistributorProperties;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Inbound adapter: time-based driver that triggers the SignalEvent processing use case.
 */
@Component
@Slf4j
public class SignalEventScheduler {

  private final SignalEventProcessingUseCase processingUseCase;
  private final boolean enable2am;
  private final boolean enableMon10;
  private final boolean enableMon12;
  private final Clock clock;

  public SignalEventScheduler(SignalEventProcessingUseCase processingUseCase,
                              DataDistributorProperties properties,
                              Clock clock) {
    this.processingUseCase = processingUseCase;
    this.enable2am = properties.getScheduler().isEnable2am();
    this.enableMon10 = properties.getScheduler().isEnableMon10();
    this.enableMon12 = properties.getScheduler().isEnableMon12();
    this.clock = clock;
  }

  /**
   * Daily 02:00 run for Tue–Sat using today's date. Skip Sunday and Monday.
   */
  @Scheduled(cron = "${data-distributor.scheduler.signal-2am-cron:0 0 2 * * *}")
  public void runDaily2am() {
    if (!enable2am) {
      log.warn("LOG_004: 2am schedule disabled via config");
      return;
    }
    LocalDate today = LocalDate.now(clock);
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
  @Scheduled(cron = "${data-distributor.scheduler.signal-mon10-cron:0 0 10 * * MON}")
  public void runMonday10am() {
    if (!enableMon10) {
      log.warn("LOG_004: Monday 10am schedule disabled via config");
      return;
    }
    LocalDate targetDate = LocalDate.now(clock).minusDays(1); // Sunday
    trigger("sched-mon-10", targetDate);
  }

  /**
   * Monday 12:00 run using today's date (Monday).
   */
  @Scheduled(cron = "${data-distributor.scheduler.signal-mon12-cron:0 0 12 * * MON}")
  public void runMondayNoon() {
    if (!enableMon12) {
      log.warn("LOG_004: Monday 12pm schedule disabled via config");
      return;
    }
    LocalDate targetDate = LocalDate.now(clock); // Monday
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
