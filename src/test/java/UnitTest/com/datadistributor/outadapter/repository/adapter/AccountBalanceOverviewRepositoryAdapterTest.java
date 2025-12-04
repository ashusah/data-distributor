package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.outadapter.entity.AccountBalanceOverviewJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceOverviewJpaRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AccountBalanceOverviewRepositoryAdapterTest {

  @Mock
  private AccountBalanceOverviewJpaRepository jpaRepository;

  private AccountBalanceOverviewRepositoryAdapter adapter;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    adapter = new AccountBalanceOverviewRepositoryAdapter(jpaRepository);
  }

  @Test
  void findBcNumberByAgreementId_returnsEmptyOnNull() {
    assertThat(adapter.findBcNumberByAgreementId(null)).isEmpty();
  }

  @Test
  void findBcNumberByAgreementId_mapsValue() {
    AccountBalanceOverviewJpaEntity entity = new AccountBalanceOverviewJpaEntity();
    entity.setBcNumber(55L);
    when(jpaRepository.findById(10L)).thenReturn(Optional.of(entity));

    assertThat(adapter.findBcNumberByAgreementId(10L)).contains(55L);
  }

  @Test
  void findByAgreementId_mapsAllFields() {
    AccountBalanceOverviewJpaEntity entity = new AccountBalanceOverviewJpaEntity();
    entity.setAgreementId(1L);
    entity.setGrv((short) 2);
    entity.setIban("iban");
    entity.setBcNumber(3L);
    entity.setCurrencyCode("EUR");
    entity.setBookDate(java.time.LocalDate.now());
    when(jpaRepository.findById(1L)).thenReturn(Optional.of(entity));

    assertThat(adapter.findByAgreementId(1L))
        .get()
        .satisfies(abo -> {
          assertThat(abo.getAgreementId()).isEqualTo(1L);
          assertThat(abo.getGrv()).isEqualTo((short) 2);
          assertThat(abo.getIban()).isEqualTo("iban");
          assertThat(abo.getBcNumber()).isEqualTo(3L);
          assertThat(abo.getCurrencyCode()).isEqualTo("EUR");
          assertThat(abo.getBookDate()).isEqualTo(entity.getBookDate());
        });
  }

  @Test
  void findByAgreementId_returnsEmptyOnNull() {
    assertThat(adapter.findByAgreementId(null)).isEmpty();
  }
}
