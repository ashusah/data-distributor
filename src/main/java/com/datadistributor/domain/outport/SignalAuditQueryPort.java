package com.datadistributor.domain.outport;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Read-only access to signal audit records for CEH deliveries.
 */
public interface SignalAuditQueryPort {

  /**
   * @return true if the latest audit for the event/consumer is PASS/SUCCESS.
   */
  boolean isEventSuccessful(Long uabsEventId, long consumerId);

  /**
   * Returns uabs event ids whose latest audit entry on the given date is not successful.
   * Used by the retry flow to re-attempt same-day failures.
   */
  List<Long> findFailedEventIdsForDate(LocalDate date);

  /**
   * Gets the latest audit status for a specific event (uabsEventId) ordered by timestamp.
   * @return Optional containing the status (PASS/FAIL/etc) if audit entry exists, empty if no entry
   */
  Optional<String> getLatestAuditStatusForEvent(Long uabsEventId, long consumerId);
}
