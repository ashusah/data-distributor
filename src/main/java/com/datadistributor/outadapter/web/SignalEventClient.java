package com.datadistributor.outadapter.web;

import com.datadistributor.domain.SignalEvent;
import reactor.core.publisher.Mono;

/**
 * Abstraction over outbound delivery implementations (blocking Feign or reactive WebClient).
 */
public interface SignalEventClient {
  Mono<ApiResponse> send(SignalEvent event);
}
