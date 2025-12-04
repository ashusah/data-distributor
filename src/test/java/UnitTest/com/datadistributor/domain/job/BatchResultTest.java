package com.datadistributor.domain.job;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BatchResult}.
 */
class BatchResultTest {

  @Test
  void emptyReturnsZeros() {
    assertThat(BatchResult.empty()).isEqualTo(new BatchResult(0, 0));
  }

  @Test
  void mergeHandlesNull() {
    BatchResult base = new BatchResult(1, 2);
    assertThat(base.merge(null)).isEqualTo(base);
  }

  @Test
  void mergeSumsCounts() {
    BatchResult merged = new BatchResult(1, 2).merge(new BatchResult(3, 4));
    assertThat(merged).isEqualTo(new BatchResult(4, 6));
  }

  @Test
  void fromBooleansCountsTrueAndFalse() {
    BatchResult result = BatchResult.fromBooleans(java.util.Arrays.asList(true, false, true, null));
    assertThat(result).isEqualTo(new BatchResult(2, 2));
  }
}
