package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.outport.InitialCehMappingPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link InitialCehQueryService}.
 */
class InitialCehQueryServiceTest {

  @Mock
  private InitialCehMappingPort port;

  private InitialCehQueryService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new InitialCehQueryService(port);
  }

  @Test
  void returnsEmptyOnNullSignalId() {
    assertThat(service.findInitialCehId(null)).isEmpty();
  }

  @Test
  void delegatesToPort() {
    when(port.findInitialCehId(7L)).thenReturn(Optional.of("abc"));

    assertThat(service.findInitialCehId(7L)).contains("abc");
  }
}
