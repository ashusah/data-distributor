package com.datadistributor.domain.job;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JobResult {

  private int successCount;
  private int failureCount;
  private long totalCount;
  private long timestamp;
  private String message;

  public JobResult(int successCount, int failureCount, String message) {
    this(successCount, failureCount, 0, message);
  }

  public JobResult(int successCount, int failureCount, long totalCount, String message) {
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.totalCount = totalCount;
    this.timestamp = System.currentTimeMillis();
    this.message = message;
  }
}
