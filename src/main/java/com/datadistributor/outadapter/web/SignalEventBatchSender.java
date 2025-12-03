package com.datadistributor.outadapter.web;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import com.datadistributor.domain.outport.SignalEventSenderPort;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Outbound adapter that posts signal events to CEH using either blocking Feign or reactive
 * WebClient, depending on configuration. Supports batch submission (async) for the main flow and
 * single-event send for retries. Persists audit entries for PASS/FAIL and maps initial CEH ids.
 */
@Component
@Slf4j
public class SignalEventBatchSender implements SignalEventBatchPort, SignalEventSenderPort {

  private final SignalEventClient blockingClient;
  private final SignalEventClient reactiveClient;
  private final InitialCehMappingUseCase initialCehMappingUseCase;
  private final SignalAuditService signalAuditService;
  private final DataDistributorProperties properties;
  private final ErrorClassifier errorClassifier;
  private final int maxConcurrentRequests;
  private static final AtomicLong SEND_SEQUENCE = new AtomicLong();

  public SignalEventBatchSender(@org.springframework.beans.factory.annotation.Qualifier("blockingSignalEventClient") SignalEventClient blockingClient,
                                @org.springframework.beans.factory.annotation.Qualifier("reactiveSignalEventClient") SignalEventClient reactiveClient,
                                InitialCehMappingUseCase initialCehMappingUseCase,
                                SignalAuditService signalAuditService,
                                DataDistributorProperties properties,
                                ErrorClassifier errorClassifier) {
    this.blockingClient = blockingClient;
    this.reactiveClient = reactiveClient;
    this.initialCehMappingUseCase = initialCehMappingUseCase;
    this.signalAuditService = signalAuditService;
    this.properties = properties;
    this.errorClassifier = errorClassifier;
    this.maxConcurrentRequests = Math.max(1, properties.getProcessing().getRateLimit());
  }

  @Override
  @Async("dataDistributorTaskExecutor")
  public CompletableFuture<BatchResult> submitBatch(List<SignalEvent> events) {
    int size = events == null ? 0 : events.size();
    if (size == 0) {
      return CompletableFuture.completedFuture(BatchResult.empty());
    }

    log.info("🚀 Sending batch of {} events | concurrencyCap={} | thread={}",
        size, maxConcurrentRequests, Thread.currentThread().getName());

    return Flux.fromIterable(events)
        .flatMap(this::postEventReactive, maxConcurrentRequests)
        .collectList()
        .map(BatchResult::fromBooleans)
        .doOnError(ex -> log.error("❌ Batch completed with errors: {}", ex.getMessage(), ex))
        .doOnSuccess(result -> log.info("✅ Batch completed: {} success / {} failure",
            result.successCount(), result.failureCount()))
        .toFuture();
  }

  private Mono<Boolean> postEventReactive(SignalEvent event) {
    Objects.requireNonNull(event, "event must not be null");
    long seq = SEND_SEQUENCE.incrementAndGet();
    String targetBaseUrl = properties.getExternalApi().getBaseUrl();
    if (seq % 1000 == 0) {
      log.info("➡️  [{}] Sending uabsEventId={} to {}", seq, event.getUabsEventId(), targetBaseUrl);
    } else {
      log.debug("➡️  [{}] Sending uabsEventId={} to {}", seq, event.getUabsEventId(), targetBaseUrl);
    }

    SignalEventClient client = properties.getExternalApi().isUseBlockingClient() ? blockingClient : reactiveClient;

    return client.send(event)
        .doOnSuccess(response -> handleSuccess(event, response))
        .map(response -> hasCehEventId(response == null ? null : response.body()))
        .onErrorResume(ex -> {
          handleException(event, ex);
          return Mono.just(false);
        });
  }

  private void handleSuccess(SignalEvent event, ApiResponse response) {
    Object ceh = response == null ? null : response.body().get("ceh_event_id");
    if (ceh != null) {
      long cehId = parseLongSafely(ceh);
      try {
        signalAuditService.persistAudit(event, "PASS", String.valueOf(response.statusCode()), "ceh_event_id=" + cehId);
      } catch (Exception ex) {
        signalAuditService.logAuditFailure(event, ex);
      }
      initialCehMappingUseCase.handleInitialCehMapping(event, cehId);
      log.info("✅ Posted uabsEventId={} | ceh_event_id={} | thread={}",
          event.getUabsEventId(), cehId, Thread.currentThread().getName());
    } else {
      try {
        signalAuditService.persistAudit(event, "FAIL", String.valueOf(response.statusCode()), "NO_CEH_EVENT_ID");
      } catch (Exception ex) {
        signalAuditService.logAuditFailure(event, ex);
      }
      log.warn("⚠️ No ceh_event_id returned for uabsEventId={} | thread={}",
          event.getUabsEventId(), Thread.currentThread().getName());
    }
  }

  private void handleException(SignalEvent event, Throwable ex) {
    FailureClassification failure = errorClassifier.classify(ex);

    log.warn("💾 Persisting FAIL for uabsEventId={} | status={} | reason={} | error={}",
        event.getUabsEventId(), failure.status(), failure.reason(), ex.toString());
    try {
      signalAuditService.persistAudit(event, failure.status(), failure.responseCode(), ex.getClass().getSimpleName() + ": " + ex.getMessage());
    } catch (Exception auditEx) {
      signalAuditService.logAuditFailure(event, auditEx);
    }

    log.error("❌ Final failure for uabsEventId={} | status={} | error={} | thread={}",
        event.getUabsEventId(), failure.status(), ex.toString(), Thread.currentThread().getName());
  }

  private long parseLongSafely(Object value) {
    if (value instanceof Number n) {
      return n.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  private boolean hasCehEventId(Map<String, Object> body) {
    return body != null && body.get("ceh_event_id") != null;
  }

  @Override
  public boolean send(SignalEvent event) {
    try {
      return postEventReactive(event).blockOptional().orElse(false);
    } catch (Exception ex) {
      log.error("❌ Retry send failed for uabsEventId={} | error={}", event.getUabsEventId(), ex.toString());
      return false;
    }
  }
}
