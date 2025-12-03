package com.datadistributor.domain.outport;

import java.time.LocalDate;
import java.util.List;

public interface SignalAuditQueryPort {

  boolean isEventSuccessful(Long uabsEventId, long consumerId);

  /**
   * Returns uabs event ids whose latest audit entry on the given date is not successful.
   */
  List<Long> findFailedEventIdsForDate(LocalDate date);
}
