package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;

class ErrorClassifierTest {

  private final ErrorClassifier classifier = new ErrorClassifier();

  @Test
  void mapsServerErrorToTransient() {
    WebClientResponseException ex = WebClientResponseException.create(
        HttpStatus.INTERNAL_SERVER_ERROR.value(), "boom", HttpHeaders.EMPTY, null, null, null);

    FailureClassification result = classifier.classify(ex);

    assertThat(result.status()).isEqualTo("FAIL_TRANSIENT");
    assertThat(result.responseCode()).isEqualTo("500");
  }

  @Test
  void mapsClientErrorToPermanent() {
    WebClientResponseException ex = WebClientResponseException.create(
        HttpStatus.BAD_REQUEST.value(), "bad", HttpHeaders.EMPTY, null, null, null);

    FailureClassification result = classifier.classify(ex);

    assertThat(result.status()).isEqualTo("FAIL_PERMANENT");
    assertThat(result.reason()).contains("BadRequest");
  }

  @Test
  void mapsCircuitOpenToBlocked() {
    CircuitBreaker breaker = CircuitBreaker.of("cb", CircuitBreakerConfig.ofDefaults());
    FailureClassification result = classifier.classify(CallNotPermittedException.createCallNotPermittedException(breaker));

    assertThat(result.status()).isEqualTo("BLOCKED_BY_CIRCUIT");
    assertThat(result.responseCode()).isEqualTo("N/A");
  }

  @Test
  void mapsIoToTransient() {
    FailureClassification result = classifier.classify(new IOException("io"));

    assertThat(result.status()).isEqualTo("FAIL_TRANSIENT");
    assertThat(result.reason()).contains("IOException");
  }
}
