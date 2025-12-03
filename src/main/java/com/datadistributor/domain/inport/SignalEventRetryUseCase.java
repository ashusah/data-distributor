package com.datadistributor.domain.inport;

import com.datadistributor.domain.job.JobResult;
import java.time.LocalDate;

/**
 * Use case to retry failed signal events for a given date.
 */
public interface SignalEventRetryUseCase {

  /**
   * Re-sends failed events of the given date and returns the outcome.
   * @param jobId optional tracking id
   * @param date target date for failures to retry
   */
  JobResult retryFailedEvents(String jobId, LocalDate date);
}
