package com.datadistributor.domain.inport;

import com.datadistributor.domain.job.JobResult;
import java.time.LocalDate;

public interface SignalEventProcessingUseCase {
  JobResult processEventsForDate(String jobId, LocalDate date);
}
