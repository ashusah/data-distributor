package com.datadistributor.domain.outport;

public interface SignalAuditQueryPort {

  boolean isEventSuccessful(Long uabsEventId, long consumerId);
}
