package com.datadistributor.outadapter.web;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.InitialCehMappingUseCase;
import com.datadistributor.domain.job.BatchResult;
import com.datadistributor.domain.outport.SignalEventBatchPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

@Component
@Slf4j
public class SignalEventBatchSender implements SignalEventBatchPort {

  private final WebClient webClient;
  private final InitialCehMappingUseCase initialCehMappingUseCase;
  private final SignalEventPayloadFactory payloadFactory;
  private final SignalAuditService signalAuditService;
  private final CircuitBreaker circuitBreaker;
  private final int maxConcurrentRequests;
  private static final AtomicLong SEND_SEQUENCE = new AtomicLong();

  @Value("${data-distributor.external-api.base-url}")
  private String externalApiBaseUrl;
  @Value("${data-distributor.external-api.write-signal-path:/create-signal/write-signal}")
  private String writeSignalPath;

  public SignalEventBatchSender(WebClient webClient,
                                InitialCehMappingUseCase initialCehMappingUseCase,
                                SignalEventPayloadFactory payloadFactory,
                                SignalAuditService signalAuditService,
                                CircuitBreakerRegistry circuitBreakerRegistry,
                                @Value("${data-distributor.processing.rate-limit:50}") int maxConcurrentRequests) {
    this.webClient = webClient;
    this.initialCehMappingUseCase = initialCehMappingUseCase;
    this.payloadFactory = payloadFactory;
    this.signalAuditService = signalAuditService;
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("signalEventApi");
    this.maxConcurrentRequests = Math.max(1, maxConcurrentRequests);
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
    if (seq % 1000 == 0) {
      log.info("➡️  [{}] Sending uabsEventId={} to {}", seq, event.getUabsEventId(), externalApiBaseUrl);
    } else {
      log.debug("➡️  [{}] Sending uabsEventId={} to {}", seq, event.getUabsEventId(), externalApiBaseUrl);
    }
    return webClient.post()
        .uri(externalApiBaseUrl + writeSignalPath)
        .bodyValue(payloadFactory.buildPayload(event))
        .exchangeToMono(clientResponse -> clientResponse
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .defaultIfEmpty(Collections.emptyMap())
            .map(body -> new ApiResponse(body, clientResponse.rawStatusCode())))
        .timeout(Duration.ofSeconds(15))
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .retryWhen(buildRetrySpec(event.getUabsEventId()))
        .doOnSuccess(response -> handleSuccess(event, response))
        .map(response -> response != null && response.body().get("ceh_event_id") != null)
        .doOnError(ex -> handleException(event, ex))
        .onErrorReturn(false);
  }

  private Retry buildRetrySpec(long uabsEventId) {
    return Retry
        .backoff(3, Duration.ofSeconds(5))
        .maxBackoff(Duration.ofSeconds(15))
        .filter(this::isRetryable)
        .doAfterRetry(retrySignal ->
            log.warn("🔁 Retry attempt #{} for uabsEventId={} cause={} breakerState={}",
                retrySignal.totalRetries() + 1,
                uabsEventId,
                retrySignal.failure() == null ? "unknown" : retrySignal.failure().toString(),
                circuitBreaker.getState()));
  }

  private boolean isRetryable(Throwable ex) {
    if (ex instanceof PrematureCloseException) return true;
    if (ex instanceof WebClientRequestException) return true;
    if (ex instanceof IOException) return true;
    if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) return true; // retry after breaker cool-down
    if (ex instanceof WebClientResponseException wcre) {
      int status = wcre.getRawStatusCode();
      if (status == 429) return true;
      if (status >= 500 && status < 600) return true;
      return false;
    }
    if (ex instanceof java.util.concurrent.TimeoutException) return true;
    return false;
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
    String status;
    String reason;

    if (ex instanceof WebClientResponseException wcre) {
      int code = wcre.getRawStatusCode();
      if (code == 429 || (code >= 500 && code < 600)) {
        status = "FAIL_TRANSIENT";
        reason = shortReason(ex, "HTTP_" + code);
      } else {
        status = "FAIL_PERMANENT";
        reason = shortReason(ex, "HTTP_" + code);
      }
    } else if (ex instanceof java.util.concurrent.TimeoutException) {
      status = "TIMEOUT";
      reason = shortReason(ex, "TIMEOUT");
    } else if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
      status = "BLOCKED_BY_CIRCUIT";
      reason = "BLOCKED_BY_CIRCUIT";
    } else if (ex instanceof WebClientRequestException
        || ex instanceof PrematureCloseException
        || ex instanceof IOException) {
      status = "FAIL_TRANSIENT";
      reason = shortReason(ex, "IO_ERROR");
    } else if (ex instanceof InterruptedException) {
      status = "INTERRUPTED";
      reason = "INTERRUPTED";
    } else {
      status = "FAIL_UNKNOWN";
      reason = shortReason(ex, "UNKNOWN");
    }

    String responseCode;
    if (ex instanceof WebClientResponseException wcrex) {
      responseCode = String.valueOf(wcrex.getRawStatusCode());
    } else {
      responseCode = "N/A";
    }

    log.warn("💾 Persisting FAIL for uabsEventId={} | status={} | reason={} | error={}",
        event.getUabsEventId(), status, reason, ex.toString());
    try {
      signalAuditService.persistAudit(event, status, responseCode, ex.getClass().getSimpleName() + ": " + ex.getMessage());
    } catch (Exception auditEx) {
      signalAuditService.logAuditFailure(event, auditEx);
    }

    log.error("❌ Final failure for uabsEventId={} | status={} | error={} | thread={}",
        event.getUabsEventId(), status, ex.toString(), Thread.currentThread().getName());
  }

  private long parseLongSafely(Object value) {
    if (value instanceof Number n) {
      return n.longValue();
    }
    return Long.parseLong(String.valueOf(value));
  }

  private String shortReason(Throwable ex, String defaultReason) {
    String cls = ex == null ? null : ex.getClass().getSimpleName();
    if (cls == null || cls.isBlank()) {
      return defaultReason;
    }
    return cls.length() > 32 ? cls.substring(0, 32) : cls;
  }

  private record ApiResponse(Map<String, Object> body, int statusCode) {
  }
}
