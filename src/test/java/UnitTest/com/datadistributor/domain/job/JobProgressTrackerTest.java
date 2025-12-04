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
}
