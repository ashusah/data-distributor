package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.outport.SignalPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link SignalQueryService}.
 */
class SignalQueryServiceTest {

  @Mock
  private SignalPort port;

  private SignalQueryService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new SignalQueryService(port);
  }

  @Test
  void findBySignalId_delegates() {
    Signal signal = new Signal();
    when(port.findBySignalId(5L)).thenReturn(Optional.of(signal));

    assertThat(service.findBySignalId(5L)).contains(signal);
  }

  @Test
  void findByAgreementId_delegates() {
    Signal signal = new Signal();
    when(port.findByAgreementId(6L)).thenReturn(Optional.of(signal));

    assertThat(service.findByAgreementId(6L)).contains(signal);
  }
}
