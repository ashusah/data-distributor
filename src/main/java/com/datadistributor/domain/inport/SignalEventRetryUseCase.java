package com.datadistributor.domain.inport;

import com.datadistributor.domain.job.JobResult;
import java.time.LocalDate;

public interface SignalEventRetryUseCase {

  JobResult retryFailedEvents(String jobId, LocalDate date);
}
