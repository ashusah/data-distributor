package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class SignalEventMapperTest {

  private final SignalEventMapper mapper = new SignalEventMapper();

  @Test
  void toDomain_nullEntityReturnsNull() {
    assertThat(mapper.toDomain((SignalEventJpaEntity) null)).isNull();
  }

  @Test
  void toDomain_mapsFields() {
    SignalEventJpaEntity entity = new SignalEventJpaEntity();
    entity.setUabsEventId(1L);
    entity.setSignalId(2L);
    entity.setAgreementId(3L);
    entity.setEventRecordDateTime(LocalDateTime.of(2024, 1, 1, 10, 0));
    entity.setEventType("TYPE");
    entity.setEventStatus("STATUS");
    entity.setUnauthorizedDebitBalance(10L);
    entity.setBookDate(LocalDate.of(2024, 1, 2));
    entity.setGrv((short) 5);
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

    List var1 = mapper.toDomain(java.util.Arrays.asList(entity, null));
    assertThat(var1).hasSize(1);
    assertThat(var1.get(0)).extracting("uabsEventId").isEqualTo(1L);
  }
}
