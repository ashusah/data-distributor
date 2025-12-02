package com.datadistributor.inadapter.rest;

import com.datadistributor.domain.job.JobResult;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class JobStatusStore {

  private final Map<String, JobResult> jobs = new ConcurrentHashMap<>();

  public void record(String jobId, JobResult result) {
    if (jobId == null || result == null) {
      return;
    }
    jobs.put(jobId, result);
  }

  public Optional<JobResult> find(String jobId) {
    if (jobId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(jobs.get(jobId));
  }
}
