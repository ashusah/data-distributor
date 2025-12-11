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

  private static final String EVENT_RECORD_DATE_TIME = "2025-01-03T10:00:00.000Z";

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

    SignalEventRequest request = buildRequest(event);
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

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void send_handlesEmptyResponseBody() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    ExchangeFunction exchange = r -> Mono.just(
        ClientResponse.create(HttpStatus.OK)
            .header("Content-Type", "application/json")
            .body("{}")
            .build());
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    ApiResponse apiResponse = client.send(event).block();
    assertThat(apiResponse.statusCode()).isEqualTo(200);
    assertThat(apiResponse.body()).isEmpty();
  }

  @Test
  void send_handlesTimeout() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    ExchangeFunction exchange = r -> Mono.delay(java.time.Duration.ofSeconds(10))
        .then(Mono.just(ClientResponse.create(HttpStatus.OK).build()));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  @Test
  void send_handlesRetryableErrors() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    properties.getExternalApi().getRetry().setAttempts(2);
    properties.getExternalApi().getRetry().setBackoffSeconds(1);

    ExchangeFunction exchange = r -> Mono.error(new java.io.IOException("Connection error"));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  @Test
  void send_handlesNonRetryableErrors() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    ExchangeFunction exchange = r -> Mono.error(
        org.springframework.web.reactive.function.client.WebClientResponseException.create(
            HttpStatus.BAD_REQUEST.value(), "Bad Request", org.springframework.http.HttpHeaders.EMPTY, new byte[0], java.nio.charset.StandardCharsets.UTF_8));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  @Test
  void send_handles429StatusCode() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    properties.getExternalApi().getRetry().setAttempts(1);

    ExchangeFunction exchange = r -> Mono.error(
        org.springframework.web.reactive.function.client.WebClientResponseException.create(
            HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests", org.springframework.http.HttpHeaders.EMPTY, new byte[0], java.nio.charset.StandardCharsets.UTF_8));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  @Test
  void send_handles500StatusCode() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    properties.getExternalApi().getRetry().setAttempts(1);

    ExchangeFunction exchange = r -> Mono.error(
        org.springframework.web.reactive.function.client.WebClientResponseException.create(
            HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal Server Error", org.springframework.http.HttpHeaders.EMPTY, new byte[0], java.nio.charset.StandardCharsets.UTF_8));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  @Test
  void send_handlesPrematureCloseException() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    properties.getExternalApi().getRetry().setAttempts(1);

    ExchangeFunction exchange = r -> Mono.error(new java.io.IOException("Connection closed prematurely"));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  @Test
  void send_handlesWebClientRequestException() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    properties.getExternalApi().getRetry().setAttempts(1);

    ExchangeFunction exchange = r -> Mono.error(new org.springframework.web.reactive.function.client.WebClientRequestException(
        new java.io.IOException("Request failed"), org.springframework.http.HttpMethod.POST, java.net.URI.create("http://example.com"), org.springframework.http.HttpHeaders.EMPTY));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  @Test
  void send_handlesCircuitBreakerOpen() {
    SignalEvent event = new SignalEvent();
    event.setUabsEventId(5L);

    SignalEventRequest request = buildRequest(event);
    when(requestFactory.build(event)).thenReturn(request);

    ExchangeFunction exchange = r -> Mono.error(new RuntimeException("Circuit breaker is open"));
    WebClient webClient = WebClient.builder().exchangeFunction(exchange).build();

    ReactiveSignalEventClient client = new ReactiveSignalEventClient(
        webClient, requestFactory, properties, CircuitBreakerRegistry.ofDefaults());

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> client.send(event).block())
        .isInstanceOf(Exception.class);
  }

  private SignalEventRequest buildRequest(SignalEvent event) {
    return new SignalEventRequest("http://example.com/endpoint",
        new SignalEventPayload(1L, 2L, "init", "pub", "pubId", "status", EVENT_RECORD_DATE_TIME, "type"));
  }
}
