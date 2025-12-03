package com.datadistributor.domain.inport;

import com.datadistributor.domain.job.JobResult;
import java.time.LocalDate;

/**
 * Use case for the main daily CEH delivery flow.
 */
public interface SignalEventProcessingUseCase {
  JobResult processEventsForDate(String jobId, LocalDate date);
}
