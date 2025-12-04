package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link InitialCehMappingService}.
 */
class InitialCehMappingServiceTest {

  @Mock
  private InitialCehMappingPort port;

  private InitialCehMappingService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new InitialCehMappingService(port);
  }

  @Test
  void skipsNonOverlimitStatus() {
    SignalEvent event = new SignalEvent();
    event.setEventStatus("FINANCIAL_UPDATE");

    service.handleInitialCehMapping(event, 1L);

    verify(port, never()).saveInitialCehMapping(anyLong(), anyLong());
  }

  @Test
  void savesWhenOverlimitAndNotPresent() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(10L);
    event.setEventStatus("OVERLIMIT_SIGNAL");
    when(port.findInitialCehId(10L)).thenReturn(Optional.empty());

    service.handleInitialCehMapping(event, 99L);

    verify(port).saveInitialCehMapping(10L, 99L);
  }

  @Test
  void ignoresWhenAlreadyMapped() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(10L);
    event.setEventStatus("OVERLIMIT_SIGNAL");
    when(port.findInitialCehId(10L)).thenReturn(Optional.of("existing"));

    service.handleInitialCehMapping(event, 99L);

    verify(port, never()).saveInitialCehMapping(anyLong(), anyLong());
  }
}
