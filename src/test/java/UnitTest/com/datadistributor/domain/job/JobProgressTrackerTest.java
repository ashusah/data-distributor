package com.datadistributor.domain.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link JobProgressTracker}.
 */
class JobProgressTrackerTest {

  private final JobProgressTracker tracker = new JobProgressTracker();

  @Test
  void startReturnsDisabledWhenInvalid() {
    JobProgressTracker.JobProgress progress = tracker.start(null, 0);
    assertThat(ReflectionTestUtils.getField(progress, "enabled")).isEqualTo(false);
  }

  @Test
  void tracksCountsAcrossBatches() {
    BatchResult first = new BatchResult(1, 0);
    BatchResult second = new BatchResult(2, 3);
    JobProgressTracker.JobProgress progress = tracker.start("job", 2);

    tracker.onBatchCompletion(progress, 1, 5, first);
    tracker.onBatchCompletion(progress, 2, 5, second);

    assertThat(ReflectionTestUtils.getField(progress, "completedBatches")).hasToString("2");
    assertThat(ReflectionTestUtils.getField(progress, "successCount")).hasToString("3");
    assertThat(ReflectionTestUtils.getField(progress, "failureCount")).hasToString("3");
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void startReturnsDisabledWhenJobIdIsNull() {
    JobProgressTracker.JobProgress progress = tracker.start(null, 5);
    assertThat(ReflectionTestUtils.getField(progress, "enabled")).isEqualTo(false);
  }

  @Test
  void startReturnsDisabledWhenTotalBatchesIsZero() {
    JobProgressTracker.JobProgress progress = tracker.start("job-1", 0);
    assertThat(ReflectionTestUtils.getField(progress, "enabled")).isEqualTo(false);
  }

  @Test
  void startReturnsDisabledWhenTotalBatchesIsNegative() {
    JobProgressTracker.JobProgress progress = tracker.start("job-1", -1);
    assertThat(ReflectionTestUtils.getField(progress, "enabled")).isEqualTo(false);
  }

  @Test
  void onBatchCompletion_skipsWhenProgressDisabled() {
    JobProgressTracker.JobProgress disabledProgress = tracker.start(null, 0);
    BatchResult result = new BatchResult(1, 0);

    tracker.onBatchCompletion(disabledProgress, 1, 5, result);

    assertThat(ReflectionTestUtils.getField(disabledProgress, "completedBatches")).hasToString("0");
  }

  @Test
  void onBatchCompletion_logsCompletionWhenAllBatchesDone() {
    BatchResult first = new BatchResult(1, 0);
    BatchResult second = new BatchResult(2, 0);
    JobProgressTracker.JobProgress progress = tracker.start("job", 2);

    tracker.onBatchCompletion(progress, 1, 5, first);
    tracker.onBatchCompletion(progress, 2, 5, second);

    assertThat(ReflectionTestUtils.getField(progress, "completedBatches")).hasToString("2");
  }

  @Test
  void onBatchCompletion_handlesZeroSuccessAndFailure() {
    BatchResult result = new BatchResult(0, 0);
    JobProgressTracker.JobProgress progress = tracker.start("job", 1);

    tracker.onBatchCompletion(progress, 1, 5, result);

    assertThat(ReflectionTestUtils.getField(progress, "successCount")).hasToString("0");
    assertThat(ReflectionTestUtils.getField(progress, "failureCount")).hasToString("0");
  }

  @Test
  void onBatchCompletion_handlesLargeCounts() {
    BatchResult result = new BatchResult(1000, 500);
    JobProgressTracker.JobProgress progress = tracker.start("job", 1);

    tracker.onBatchCompletion(progress, 1, 100, result);

    assertThat(ReflectionTestUtils.getField(progress, "successCount")).hasToString("1000");
    assertThat(ReflectionTestUtils.getField(progress, "failureCount")).hasToString("500");
  }

  @Test
  void onBatchCompletion_handlesMultipleBatches() {
    BatchResult first = new BatchResult(10, 2);
    BatchResult second = new BatchResult(15, 3);
    BatchResult third = new BatchResult(20, 5);
    JobProgressTracker.JobProgress progress = tracker.start("job", 3);

    tracker.onBatchCompletion(progress, 1, 12, first);
    tracker.onBatchCompletion(progress, 2, 18, second);
    tracker.onBatchCompletion(progress, 3, 25, third);

    assertThat(ReflectionTestUtils.getField(progress, "completedBatches")).hasToString("3");
    assertThat(ReflectionTestUtils.getField(progress, "successCount")).hasToString("45");
    assertThat(ReflectionTestUtils.getField(progress, "failureCount")).hasToString("10");
  }
}
