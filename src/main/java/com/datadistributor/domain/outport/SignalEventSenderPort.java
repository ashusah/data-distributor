package com.datadistributor.domain.outport;

import com.datadistributor.domain.SignalEvent;

/**
 * Sends a single signal event synchronously, returning true on success. Implementations can wrap
 * blocking or reactive clients but should not return until the attempt completes.
 */
public interface SignalEventSenderPort {
  boolean send(SignalEvent event);
}
