package com.datadistributor.inadapter.rest;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.domain.job.JobResult;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for the in-memory job status store.
 */
class JobStatusStoreTest {

  private final JobStatusStore store = new JobStatusStore();

  @Test
  void recordIgnoresNulls() {
    store.record(null, null);
    assertThat(store.find(null)).isEmpty();
  }

  @Test
  void findReturnsStoredJob() {
    JobResult result = new JobResult(1, 0, 0, "ok");
    store.record("job-1", result);

    assertThat(store.find("job-1")).containsSame(result);
  }

  @Test
  void findReturnsEmptyForUnknown() {
    assertThat(store.find("missing")).isEmpty();
  }
}
