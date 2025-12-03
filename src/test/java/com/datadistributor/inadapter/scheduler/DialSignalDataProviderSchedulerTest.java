package com.datadistributor.inadapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.service.DialSignalDataExportService;
import com.datadistributor.domain.inport.SignalEventUseCase;
import com.datadistributor.domain.outport.FileStoragePort;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DialSignalDataProviderSchedulerTest {

  @Test
  void runsWithConfiguredOffset() {
    var fixedClock = Clock.fixed(Instant.parse("2025-12-02T06:00:00Z"), ZoneOffset.UTC);
    var exportService = new CapturingExportService();
    var scheduler = new DialSignalDataProviderScheduler(exportService, 2, fixedClock, enabledProps());

    scheduler.runDialExport();

    assertThat(exportService.lastDate).isEqualTo(LocalDate.of(2025, 12, 4));
  }

  private static class CapturingExportService extends DialSignalDataExportService {
    LocalDate lastDate;

    CapturingExportService() {
      super(dummyUseCase(), dummyStorage(), new DataDistributorProperties());
    }

    @Override
    public void export(LocalDate date) {
      this.lastDate = date;
    }

    private static SignalEventUseCase dummyUseCase() {
      return new SignalEventUseCase() {
        @Override
        public List<com.datadistributor.domain.SignalEvent> getAllSignalEventsOfThisDate(LocalDate date) {
          return List.of();
        }

        @Override
        public List<com.datadistributor.domain.SignalEvent> getAllSignalForCEH(LocalDate date) {
          return List.of();
        }
      };
    }

    private static FileStoragePort dummyStorage() {
      return (folder, fileName, content) -> {};
    }
  }

  private static DataDistributorProperties enabledProps() {
    DataDistributorProperties props = new DataDistributorProperties();
    props.getStorage().setDialSchedulerEnabled(true);
    return props;
  }
}
