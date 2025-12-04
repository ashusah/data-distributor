package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

class ReactiveSignalEventClientTest {

  @Mock
  private SignalEventRequestFactory requestFactory;

  private DataDistributorProperties properties;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
    properties.getExternalApi().setRequestTimeoutSeconds(5);
    properties.getExternalApi().getRetry().setAttempts(0);
  }

  @Test
  void send_returnsResponseBodyAndStatus() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = new SignalEventRequest("http://example.com/endpoint",
        new SignalEventPayload(1L, 2L, "init", "pub", "pubId", "status", java.time.LocalDateTime.now(), "type"));
    when(requestFactory.build(event)).thenReturn(request);

    ExchangeFunction exchange = r -> Mono.just(
        ClientResponse.create(HttpStatus.ACCEPTED)
            .header("Content-Type", "application/json")
            .body("{\"ceh_event_id\":42}")
            .build());
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    ApiResponse apiResponse = client.send(event).block();
    assertThat(apiResponse.statusCode()).isEqualTo(202);
    assertThat(apiResponse.body()).containsEntry("ceh_event_id", 42);
  }
}
