package com.datadistributor.domain.job;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link JobResult}.
 */
class JobResultTest {

  @Test
  void constructorsPopulateFields() {
    JobResult threeArg = new JobResult(1, 2, "msg");
    assertThat(threeArg.getSuccessCount()).isEqualTo(1);
    assertThat(threeArg.getFailureCount()).isEqualTo(2);
    assertThat(threeArg.getTotalCount()).isEqualTo(0);
    assertThat(threeArg.getTimestamp()).isPositive();
    assertThat(threeArg.getMessage()).isEqualTo("msg");

    JobResult fourArg = new JobResult(3, 4, 10L, "ok");
    assertThat(fourArg.getTotalCount()).isEqualTo(10);
    assertThat(fourArg.getSuccessCount()).isEqualTo(3);
    assertThat(fourArg.getFailureCount()).isEqualTo(4);
    assertThat(fourArg.getMessage()).isEqualTo("ok");
  }
}
