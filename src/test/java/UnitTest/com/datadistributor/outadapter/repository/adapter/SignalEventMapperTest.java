package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SignalEventMapperTest {

  private final SignalEventMapper mapper = Mappers.getMapper(SignalEventMapper.class);

  @Test
  void toDomain_nullEntityReturnsNull() {
    assertThat(mapper.toDomain((SignalEventJpaEntity) null)).isNull();
  }

  @Test
  void toDomain_mapsFields() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setUabsEventId(1L);
    
    SignalJpaEntity signal = new SignalJpaEntity();
    signal.setSignalId(2L);
    entity.setSignal(signal);
    
    AccountBalanceJpaEntity account = new AccountBalanceJpaEntity();
    account.setAgreementId(3L);
    entity.setAccountBalance(account);
    
    entity.setEventRecordDateTime(LocalDateTime.of(2024, 1, 1, 10, 0));
    entity.setEventType("TYPE");
    entity.setEventStatus("STATUS");
    entity.setUnauthorizedDebitBalance(10L);
    entity.setBookDate(LocalDate.of(2024, 1, 2));
    ProductRiskMonitoringJpaEntity prm = new ProductRiskMonitoringJpaEntity();
    prm.setGrv((short) 5);
    prm.setProductId((short) 1);
    prm.setCurrencyCode("EUR");
    prm.setMonitorCW014Signal("Y");
    prm.setMonitorKraandicht("Y");
    prm.setReportCW014ToCEH("Y");
    prm.setReportCW014ToDial("Y");
    entity.setGrv(prm);
    entity.setProductId((short) 6);

    var domain = mapper.toDomain(entity);

    assertThat(domain.getUabsEventId()).isEqualTo(1L);
    assertThat(domain.getSignalId()).isEqualTo(2L);
    assertThat(domain.getAgreementId()).isEqualTo(3L);
    assertThat(domain.getEventRecordDateTime()).isEqualTo(entity.getEventRecordDateTime());
    assertThat(domain.getEventType()).isEqualTo("TYPE");
    assertThat(domain.getEventStatus()).isEqualTo("STATUS");
    assertThat(domain.getUnauthorizedDebitBalance()).isEqualTo(10L);
    assertThat(domain.getBookDate()).isEqualTo(entity.getBookDate());
    assertThat(domain.getGrv()).isEqualTo((short) 5);
    assertThat(domain.getProductId()).isEqualTo((short) 6);
  }

  @Test
  void toDomainList_filtersNullsAndMaps() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setUabsEventId(1L);

    List<?> mapped = mapper.toDomainList(java.util.Arrays.asList(entity, null));
    assertThat(mapped).hasSize(2);
    assertThat(mapped.get(0)).extracting("uabsEventId").isEqualTo(1L);
    assertThat(mapped.get(1)).isNull();
  }
}
