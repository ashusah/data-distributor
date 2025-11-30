package com.datadistributor.domain.outport;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.job.BatchResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface SignalEventBatchPort {
  CompletableFuture<BatchResult> submitBatch(List<SignalEvent> events);
}
