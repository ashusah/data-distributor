package com.datadistributor.support;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Provides a deterministic stub for the outbound API used during integration tests.
 */
@TestConfiguration
public class StubExternalApiConfig {

  @Bean
  @Primary
  WebClient webClient() {
    ExchangeFunction fx = request -> {
      long cehId = 100_000_000L + ThreadLocalRandom.current().nextLong(900_000_000L);
      ClientResponse response = ClientResponse.create(HttpStatus.OK)
          .header("Content-Type", "application/json")
          .body("{\"ceh_event_id\": " + cehId + "}")
          .build();
      return Mono.just(response);
    };
    return WebClient.builder().exchangeFunction(fx).build();
  }
}
