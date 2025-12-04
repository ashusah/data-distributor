package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.AccountBalanceJpaRepository;
import org.mapstruct.factory.Mappers;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AccountBalanceOverviewRepositoryAdapterTest {

  @Mock
  private AccountBalanceJpaRepository jpaRepository;

  private AccountBalanceOverviewRepositoryAdapter adapter;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    adapter = new AccountBalanceOverviewRepositoryAdapter(jpaRepository, Mappers.getMapper(AccountBalanceMapper.class));
  }

  @Test
  void findBcNumberByAgreementId_returnsEmptyOnNull() {
    assertThat(adapter.findBcNumberByAgreementId(null)).isEmpty();
  }

  @Test
  void findBcNumberByAgreementId_mapsValue() {
    AccountBalanceJpaEntity entity = new AccountBalanceJpaEntity();
    entity.setBcNumber(55L);
    when(jpaRepository.findById(10L)).thenReturn(Optional.of(entity));

    assertThat(adapter.findBcNumberByAgreementId(10L)).contains(55L);
  }

  @Test
  void findByAgreementId_mapsAllFields() {
    AccountBalanceJpaEntity entity = new AccountBalanceJpaEntity();
    entity.setAgreementId(1L);
    ProductRiskMonitoringJpaEntity prm = new ProductRiskMonitoringJpaEntity();
    prm.setGrv((short) 2);
    prm.setProductId((short) 9);
    prm.setCurrencyCode("EUR");
    prm.setMonitorCW014Signal("Y");
    prm.setMonitorKraandicht("Y");
    prm.setReportCW014ToCEH("Y");
    prm.setReportCW014ToDial("Y");
    entity.setGrv(prm);
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
          assertThat(abo.getProductId()).isEqualTo("9");
        });
  }

  @Test
  void findByAgreementId_returnsEmptyOnNull() {
    assertThat(adapter.findByAgreementId(null)).isEmpty();
  }
}
