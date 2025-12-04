package com.datadistributor.inadapter.scheduler;

import com.datadistributor.domain.service.DialSignalDataExportDomainService;
import com.datadistributor.application.config.DataDistributorProperties;
import java.time.Clock;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Triggers the DIAL signal data export on a configured schedule with an optional day offset.
 */
@Component
@Slf4j
public class DialSignalDataProviderScheduler {

  private final DialSignalDataExportDomainService exportService;
  private final int dayOffset;
  private final Clock clock;
  private final boolean enabled;

  public DialSignalDataProviderScheduler(
      DialSignalDataExportDomainService exportService,
      @Value("${data-distributor.scheduler.dial-signal-day-offset:0}") int dayOffset,
      Clock clock,
      DataDistributorProperties properties) {
    this.exportService = exportService;
    this.dayOffset = dayOffset;
    this.clock = clock;
    this.enabled = properties.getStorage().isDialSchedulerEnabled();
  }

  @Scheduled(cron = "${data-distributor.scheduler.dial-signal-cron:0 0 6 * * *}")
  public void runDialExport() {
    if (!enabled) {
      log.info("DialSignalDataProvider skipped because dialSchedulerEnabled=false");
      return;
    }
    LocalDate targetDate = LocalDate.now(clock).plusDays(dayOffset);
    log.info("DialSignalDataProvider running for date {}", targetDate);
    exportService.export(targetDate);
  }
}
