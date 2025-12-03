package com.datadistributor.domain.inport;

import com.datadistributor.domain.SignalEvent;
import java.time.LocalDate;
import java.util.List;

public interface SignalDispatchSelectorUseCase {
  List<SignalEvent> selectEventsToSend(LocalDate targetDate);
}
