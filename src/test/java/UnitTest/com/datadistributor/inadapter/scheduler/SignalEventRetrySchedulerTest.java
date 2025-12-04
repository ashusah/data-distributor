package com.datadistributor.inadapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.inport.SignalEventRetryUseCase;
import com.datadistributor.domain.job.JobResult;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class SignalEventRetrySchedulerTest {

  @Test
  void skipsWhenDisabled() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getScheduler().setEnableRetry(false);
    FakeRetryUseCase retryUseCase = new FakeRetryUseCase();
    Clock clock = Clock.fixed(ZonedDateTime.of(2024, 12, 3, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC"));

    SignalEventRetryScheduler scheduler = new SignalEventRetryScheduler(retryUseCase, properties, clock);
    scheduler.retryHourly();

    assertThat(retryUseCase.lastDate).isNull();
  }

  @Test
  void skipsOnMonday() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getScheduler().setEnableRetry(true);
    FakeRetryUseCase retryUseCase = new FakeRetryUseCase();
    Clock clock = Clock.fixed(ZonedDateTime.of(2024, 12, 2, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC")); // Monday

    SignalEventRetryScheduler scheduler = new SignalEventRetryScheduler(retryUseCase, properties, clock);
    scheduler.retryHourly();

    assertThat(retryUseCase.lastDate).isNull();
  }

  @Test
  void triggersOnAllowedDay() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getScheduler().setEnableRetry(true);
    FakeRetryUseCase retryUseCase = new FakeRetryUseCase();
    Clock clock = Clock.fixed(ZonedDateTime.of(2024, 12, 3, 14, 0, 0, 0, ZoneOffset.UTC).toInstant(), ZoneId.of("UTC")); // Tuesday

    SignalEventRetryScheduler scheduler = new SignalEventRetryScheduler(retryUseCase, properties, clock);
    scheduler.retryHourly();

    assertThat(retryUseCase.lastDate).isEqualTo(LocalDate.of(2024, 12, 3));
  }

  private static class FakeRetryUseCase implements SignalEventRetryUseCase {
    LocalDate lastDate;

    @Override
    public JobResult retryFailedEvents(String jobId, LocalDate date) {
      lastDate = date;
      return new JobResult(0, 0, 0, "ok");
    }
  }
}
