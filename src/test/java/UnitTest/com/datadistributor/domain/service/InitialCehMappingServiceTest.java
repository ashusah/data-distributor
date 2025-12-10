package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.outport.InitialCehMappingPort;
import com.datadistributor.domain.service.InitialCehMappingDomainService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link InitialCehMappingDomainService}.
 */
class InitialCehMappingServiceTest {

  @Mock
  private InitialCehMappingPort port;

  private InitialCehMappingDomainService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new InitialCehMappingDomainService(port);
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

  // ***************************************************
  // NEW TEST- Date- Dec 9
  // ***************************************************

  @Test
  void handlesNullEvent() {
    service.handleInitialCehMapping(null, 1L);
    verify(port, never()).saveInitialCehMapping(anyLong(), anyLong());
  }

  @Test
  void handlesNullEventStatus() {
    SignalEvent event = new SignalEvent();
    event.setEventStatus(null);
    service.handleInitialCehMapping(event, 1L);
    verify(port, never()).saveInitialCehMapping(anyLong(), anyLong());
  }

  @Test
  void handlesNullSignalId() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(null);
    event.setEventStatus("OVERLIMIT_SIGNAL");
    service.handleInitialCehMapping(event, 1L);
    verify(port, never()).saveInitialCehMapping(anyLong(), anyLong());
  }

  @Test
  void handlesExceptionDuringSave() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(10L);
    event.setEventStatus("OVERLIMIT_SIGNAL");
    when(port.findInitialCehId(10L)).thenReturn(Optional.empty());
    doThrow(new RuntimeException("Database error")).when(port).saveInitialCehMapping(10L, 99L);

    assertThatCode(() -> service.handleInitialCehMapping(event, 99L)).doesNotThrowAnyException();
    verify(port).saveInitialCehMapping(10L, 99L);
  }

  // **********************************************************
  // ADDITIONAL TEST
  // **********************************************************

  @Test
  void handlesOverlimitStatusCaseInsensitively() {
    SignalEvent event = new SignalEvent();
    event.setSignalId(10L);
    event.setEventStatus("overlimit_signal");
    when(port.findInitialCehId(10L)).thenReturn(Optional.empty());

    service.handleInitialCehMapping(event, 99L);

    verify(port).saveInitialCehMapping(10L, 99L);
  }
}
