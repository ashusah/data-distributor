package com.datadistributor.domain.outport;

import com.datadistributor.domain.SignalEvent;

/**
 * Sends a single signal event synchronously, returning true on success.
 */
public interface SignalEventSenderPort {
  boolean send(SignalEvent event);
}
