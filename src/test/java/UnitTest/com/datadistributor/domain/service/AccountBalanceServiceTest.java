package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.AccountBalance;
import com.datadistributor.domain.outport.AccountBalanceOverviewPort;
import com.datadistributor.domain.service.AccountBalanceDomainService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link AccountBalanceDomainService}.
 */
class AccountBalanceServiceTest {

  @Mock
  private AccountBalanceOverviewPort port;

  private AccountBalanceDomainService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new AccountBalanceDomainService(port);
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

  @Test
  void returnsNullBalanceWhenNotFound() {
    assertThat(service.getAccountBalanceOfAgreement(1L)).isEmpty();
  }

  @Test
  void returnsBalanceWhenPresent() {
    AccountBalance balance = new AccountBalance();
    balance.setAgreementId(1L);
    when(port.findByAgreementId(1L)).thenReturn(Optional.of(balance));

    assertThat(service.getAccountBalanceOfAgreement(1L)).containsSame(balance);
  }
}
