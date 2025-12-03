package com.datadistributor.domain.outport;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.job.BatchResult;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Outbound port to deliver a batch of signal events asynchronously.
 * Implementations may use blocking or reactive HTTP clients, but must return a future
 * summarizing success/failure counts for the batch.
 */
public interface SignalEventBatchPort {
  CompletableFuture<BatchResult> submitBatch(List<SignalEvent> events);
}
