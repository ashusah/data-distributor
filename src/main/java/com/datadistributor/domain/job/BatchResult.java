package com.datadistributor.domain.job;

/**
 * Immutable summary for a single batch submission (counts of success/failure).
 */
public record BatchResult(int successCount, int failureCount) {

  public static BatchResult empty() {
    return new BatchResult(0, 0);
  }

  public BatchResult merge(BatchResult other) {
    if (other == null) {
      return this;
    }
    return new BatchResult(this.successCount + other.successCount,
        this.failureCount + other.failureCount);
  }

  public static BatchResult fromBooleans(Iterable<Boolean> results) {
    int success = 0;
    int failure = 0;
    if (results != null) {
      for (Boolean result : results) {
        if (Boolean.TRUE.equals(result)) {
          success++;
        } else {
          failure++;
        }
      }
    }
    return new BatchResult(success, failure);
  }
}
