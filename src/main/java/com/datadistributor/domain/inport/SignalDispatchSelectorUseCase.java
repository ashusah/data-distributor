package com.datadistributor.domain.inport;

import com.datadistributor.domain.SignalEvent;
import java.time.LocalDate;
import java.util.List;

/**
 * Use case for selecting which signal events should be sent on a given processing date.
 */
public interface SignalDispatchSelectorUseCase {
  List<SignalEvent> selectEventsToSend(LocalDate targetDate);
}
