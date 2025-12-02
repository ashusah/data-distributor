package com.datadistributor.support;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
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
  WebClient webClient(@Value("${stub.api.response-mode:success}") String mode) {
    ExchangeFunction fx = request -> {
      if ("fail".equalsIgnoreCase(mode)) {
        ClientResponse response = ClientResponse.create(HttpStatus.INTERNAL_SERVER_ERROR)
            .header("Content-Type", "application/json")
            .body("{\"error\": \"simulated\"}")
            .build();
        return Mono.just(response);
      }
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
