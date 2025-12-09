package com.datadistributor.inadapter.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.inport.SignalEventProcessingUseCase;
import com.datadistributor.domain.job.JobResult;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for the signal event scheduler covering enable flags and date selection.
 */
class SignalEventSchedulerTest {

  @Mock
  private SignalEventProcessingUseCase processingUseCase;

  private DataDistributorProperties properties;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
    when(processingUseCase.processEventsForDate(any(), any())).thenReturn(new JobResult(0, 0, 0, "ok"));
  }

  private Clock fixed(LocalDate date) {
    return Clock.fixed(date.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
  }

  @Test
  void runDaily2am_skipsWhenDisabled() {
    properties.getScheduler().setEnable2am(false);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 3)));

    scheduler.runDaily2am();

    verify(processingUseCase, never()).processEventsForDate(any(), any());
  }

  @Test
  void runDaily2am_skipsSundayAndMonday() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler sunday = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 1))); // Sunday
    SignalEventScheduler monday = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 2))); // Monday

    sunday.runDaily2am();
    monday.runDaily2am();

    verify(processingUseCase, never()).processEventsForDate(any(), any());
  }

  @Test
  void runDaily2am_triggersOnWeekday() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 3))); // Tuesday

    scheduler.runDaily2am();

    ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
    verify(processingUseCase).processEventsForDate(any(), captor.capture());
    assertThat(captor.getValue()).isEqualTo(LocalDate.of(2024, 12, 3));
  }

  @Test
  void runMonday10am_usesPreviousDay() {
    properties.getScheduler().setEnableMon10(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 2))); // Monday

    scheduler.runMonday10am();

    ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
    verify(processingUseCase).processEventsForDate(any(), captor.capture());
    assertThat(captor.getValue()).isEqualTo(LocalDate.of(2024, 12, 1));
  }

  @Test
  void runMondayNoon_usesCurrentDay() {
    properties.getScheduler().setEnableMon12(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 2))); // Monday

    scheduler.runMondayNoon();

    ArgumentCaptor<LocalDate> captor = ArgumentCaptor.forClass(LocalDate.class);
    verify(processingUseCase).processEventsForDate(any(), captor.capture());
    assertThat(captor.getValue()).isEqualTo(LocalDate.of(2024, 12, 2));
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void runDaily2am_triggersOnTuesday() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 3))); // Tuesday

    scheduler.runDaily2am();

    verify(processingUseCase).processEventsForDate(any(), eq(LocalDate.of(2024, 12, 3)));
  }

  @Test
  void runDaily2am_triggersOnWednesday() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 4))); // Wednesday

    scheduler.runDaily2am();

    verify(processingUseCase).processEventsForDate(any(), eq(LocalDate.of(2024, 12, 4)));
  }

  @Test
  void runDaily2am_triggersOnThursday() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 5))); // Thursday

    scheduler.runDaily2am();

    verify(processingUseCase).processEventsForDate(any(), eq(LocalDate.of(2024, 12, 5)));
  }

  @Test
  void runDaily2am_triggersOnFriday() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 6))); // Friday

    scheduler.runDaily2am();

    verify(processingUseCase).processEventsForDate(any(), eq(LocalDate.of(2024, 12, 6)));
  }

  @Test
  void runDaily2am_triggersOnSaturday() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 7))); // Saturday

    scheduler.runDaily2am();

    verify(processingUseCase).processEventsForDate(any(), eq(LocalDate.of(2024, 12, 7)));
  }

  @Test
  void runMonday10am_skipsWhenDisabled() {
    properties.getScheduler().setEnableMon10(false);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 2))); // Monday

    scheduler.runMonday10am();

    verify(processingUseCase, never()).processEventsForDate(any(), any());
  }

  @Test
  void runMondayNoon_skipsWhenDisabled() {
    properties.getScheduler().setEnableMon12(false);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 2))); // Monday

    scheduler.runMondayNoon();

    verify(processingUseCase, never()).processEventsForDate(any(), any());
  }

  @Test
  void trigger_generatesUniqueJobId() {
    properties.getScheduler().setEnable2am(true);
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 3)));

    scheduler.runDaily2am();

    ArgumentCaptor<String> jobIdCaptor = ArgumentCaptor.forClass(String.class);
    verify(processingUseCase).processEventsForDate(jobIdCaptor.capture(), any());
    assertThat(jobIdCaptor.getValue()).startsWith("sched-2am-");
  }

  @Test
  void trigger_logsResult() {
    properties.getScheduler().setEnable2am(true);
    when(processingUseCase.processEventsForDate(any(), any()))
        .thenReturn(new JobResult(5, 2, 7, "Done"));
    SignalEventScheduler scheduler = new SignalEventScheduler(processingUseCase, properties, fixed(LocalDate.of(2024, 12, 3)));

    scheduler.runDaily2am();

    verify(processingUseCase).processEventsForDate(any(), eq(LocalDate.of(2024, 12, 3)));
  }
}
