package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link AccountBalanceQueryService}.
 */
class AccountBalanceQueryServiceTest {

  @Mock
  private AccountBalanceOverviewPort port;

  private AccountBalanceQueryService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new AccountBalanceQueryService(port);
  }

  @Test
  void returnsEmptyOnNullInput() {
    assertThat(service.findBcNumberByAgreementId(null)).isEmpty();
  }

  @Test
  void delegatesToPort() {
    when(port.findBcNumberByAgreementId(10L)).thenReturn(Optional.of(99L));

    assertThat(service.findBcNumberByAgreementId(10L)).contains(99L);
  }
}
