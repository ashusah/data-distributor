package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.Signal;
import com.datadistributor.domain.outport.SignalPort;
import com.datadistributor.domain.service.SignalQueryDomainService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link SignalQueryDomainService}.
 */
class SignalQueryServiceTest {

  @Mock
  private SignalPort port;

  private SignalQueryDomainService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new SignalQueryDomainService(port);
  }

  @Test
  void findBySignalId_delegates() {
    Signal signal = new Signal();
    when(port.findBySignalId(5L)).thenReturn(Optional.of(signal));

    assertThat(service.findBySignalId(5L)).contains(signal);
  }

  @Test
  void getOpenSignalOfAgreement_delegates() {
    Signal signal = new Signal();
    when(port.getOpenSignalOfAgreement(6L)).thenReturn(Optional.of(signal));

    assertThat(service.getOpenSignalOfAgreement(6L)).contains(signal);
  }
}
