package com.datadistributor.outadapter.web;

import com.datadistributor.domain.SignalEvent;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Qualifier;
import reactor.core.publisher.Mono;

@Component("blockingSignalEventClient")
public class BlockingSignalEventClient implements SignalEventClient {

  private final SignalEventFeignClient feignClient;
  private final SignalEventRequestFactory requestFactory;

  public BlockingSignalEventClient(SignalEventFeignClient feignClient,
                                   SignalEventRequestFactory requestFactory) {
    this.feignClient = feignClient;
    this.requestFactory = requestFactory;
  }

  @Override
  public Mono<ApiResponse> send(SignalEvent event) {
    return Mono.fromCallable(() -> {
      SignalEventRequest request = requestFactory.build(event);
      SignalEventResponse response = feignClient.postSignalEvent(request.payload());
      return new ApiResponse(toResponseBody(response), 200);
    });
  }

  private Map<String, Object> toResponseBody(SignalEventResponse response) {
    if (response == null) {
      return Map.of();
    }
    Map<String, Object> body = new HashMap<>();
    if (response.cehEventId() != null) {
      body.put("ceh_event_id", response.cehEventId());
    }
    return body;
  }
}
