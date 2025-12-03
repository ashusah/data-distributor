package com.datadistributor.outadapter.web;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.util.retry.Retry;

/**
 * Reactive implementation using WebClient with retry and circuit breaker. Honors timeout and retry
 * settings from properties. Used when non-blocking client is configured.
 */
@Component("reactiveSignalEventClient")
@Slf4j
public class ReactiveSignalEventClient implements SignalEventClient {

  private final WebClient webClient;
  private final SignalEventRequestFactory requestFactory;
  private final DataDistributorProperties properties;
  private final CircuitBreaker circuitBreaker;

  public ReactiveSignalEventClient(WebClient webClient,
                                   SignalEventRequestFactory requestFactory,
                                   DataDistributorProperties properties,
                                   CircuitBreakerRegistry circuitBreakerRegistry) {
    this.webClient = webClient;
    this.requestFactory = requestFactory;
    this.properties = properties;
    this.circuitBreaker = circuitBreakerRegistry.circuitBreaker("signalEventApi");
  }

  @Override
  public Mono<ApiResponse> send(SignalEvent event) {
    SignalEventRequest request = requestFactory.build(event);
    return webClient.post()
        .uri(request.uri())
        .bodyValue(request.payload())
        .exchangeToMono(clientResponse -> clientResponse
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
            .defaultIfEmpty(Collections.emptyMap())
            .map(body -> new ApiResponse(body, clientResponse.rawStatusCode())))
        .timeout(Duration.ofSeconds(properties.getExternalApi().getRequestTimeoutSeconds()))
        .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
        .retryWhen(buildRetrySpec(event.getUabsEventId()));
  }

  private Retry buildRetrySpec(long uabsEventId) {
    var retry = properties.getExternalApi().getRetry();
    return Retry
        .backoff(retry.getAttempts(), Duration.ofSeconds(retry.getBackoffSeconds()))
        .maxBackoff(Duration.ofSeconds(retry.getMaxBackoffSeconds()))
        .filter(this::isRetryable)
        .doAfterRetry(retrySignal -> log.warn(
            "🔁 Retry attempt #{} for uabsEventId={} cause={} breakerState={}",
            retrySignal.totalRetries() + 1,
            uabsEventId,
            retrySignal.failure() == null ? "unknown" : retrySignal.failure().toString(),
            circuitBreaker.getState()));
  }

  private boolean isRetryable(Throwable ex) {
    if (ex instanceof PrematureCloseException) return true;
    if (ex instanceof WebClientRequestException) return true;
    if (ex instanceof IOException) return true;
    if (ex instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) return false;
    if (ex instanceof WebClientResponseException wcre) {
      int status = wcre.getRawStatusCode();
      if (status == 429) return true;
      if (status >= 500 && status < 600) return true;
      return false;
    }
    if (ex instanceof java.util.concurrent.TimeoutException) return true;
    return false;
  }
}
