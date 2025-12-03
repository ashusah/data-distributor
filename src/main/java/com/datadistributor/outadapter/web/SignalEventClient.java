package com.datadistributor.outadapter.web;

import com.datadistributor.domain.SignalEvent;
import reactor.core.publisher.Mono;

public interface SignalEventClient {
  Mono<ApiResponse> send(SignalEvent event);
}
