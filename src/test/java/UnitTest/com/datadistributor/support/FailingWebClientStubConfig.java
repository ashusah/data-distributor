package com.datadistributor.support;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * Shared failing WebClient stub for integration tests that need controlled retries/call counts.
 */
@TestConfiguration
public class FailingWebClientStubConfig {

  @Bean
  AtomicInteger stubCallCount() {
    return new AtomicInteger();
  }

  @Bean
  @Primary
  WebClient webClient(AtomicInteger stubCallCount) {
    ExchangeFunction fx = request -> Mono.defer(() -> {
      stubCallCount.incrementAndGet();
      return Mono.error(WebClientResponseException.create(
          HttpStatus.INTERNAL_SERVER_ERROR.value(),
          "simulated-500",
          HttpHeaders.EMPTY,
          null,
          null,
          null));
    });
    return WebClient.builder().exchangeFunction(fx).build();
  }
}
