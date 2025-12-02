package com.datadistributor.inadapter.scheduler;

import com.datadistributor.domain.service.DialSignalDataExportService;
import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DialSignalDataProviderScheduler {

  private final DialSignalDataExportService exportService;
  private final int dayOffset;

  public DialSignalDataProviderScheduler(
      DialSignalDataExportService exportService,
      @Value("${data-distributor.scheduler.dial-signal-day-offset:0}") int dayOffset) {
    this.exportService = exportService;
    this.dayOffset = dayOffset;
  }

  @Scheduled(cron = "${data-distributor.scheduler.dial-signal-cron:0 0 6 * * *}")
  public void runDialExport() {
    LocalDate targetDate = LocalDate.now().plusDays(dayOffset);
    log.info("DialSignalDataProvider running for date {}", targetDate);
    exportService.export(targetDate);
  }
}
