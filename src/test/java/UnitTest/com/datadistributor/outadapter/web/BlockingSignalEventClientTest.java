package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.SignalEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class BlockingSignalEventClientTest {

  @Mock
  private SignalEventFeignClient feignClient;
  @Mock
  private SignalEventRequestFactory requestFactory;

  private BlockingSignalEventClient client;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    client = new BlockingSignalEventClient(feignClient, requestFactory);
  }

  @Test
  void send_wrapsFeignResponse() {
    SignalEvent event = new SignalEvent();
    SignalEventPayload payload = new SignalEventPayload(1L, 2L, "init", "pub", "pubId", "status", java.time.LocalDateTime.now(), "type");
    SignalEventRequest request = new SignalEventRequest("http://example", payload);
    when(requestFactory.build(event)).thenReturn(request);
    when(feignClient.postSignalEvent(payload)).thenReturn(new SignalEventResponse(99L));

    ApiResponse response = client.send(event).block();
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.body()).containsEntry("ceh_event_id", 99L);
  }

  @Test
  void send_handlesNullResponse() {
    SignalEvent event = new SignalEvent();
    SignalEventPayload payload = new SignalEventPayload(1L, 2L, null, "pub", "pubId", "status", java.time.LocalDateTime.now(), "type");
    SignalEventRequest request = new SignalEventRequest("http://example", payload);
    when(requestFactory.build(event)).thenReturn(request);
    when(feignClient.postSignalEvent(payload)).thenReturn(null);

    ApiResponse response = client.send(event).block();
    assertThat(response.body()).isEmpty();
  }
}
