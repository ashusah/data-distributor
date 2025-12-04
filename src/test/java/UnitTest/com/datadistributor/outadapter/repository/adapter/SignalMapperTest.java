package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.outadapter.entity.SignalJpaEntity;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class SignalMapperTest {

  private final SignalMapper mapper = Mappers.getMapper(SignalMapper.class);

  @Test
  void toDomain_returnsNullWhenEntityNull() {
    assertThat(mapper.toDomain(null)).isNull();
  }

  @Test
  void toDomain_mapsFields() {
    SignalJpaEntity entity = new SignalJpaEntity();
    entity.setSignalId(1L);
    entity.setAgreementId(2L);
    entity.setSignalStartDate(LocalDate.of(2025, 1, 1));
    entity.setSignalEndDate(LocalDate.of(2025, 1, 5));

    var domain = mapper.toDomain(entity);

    assertThat(domain.getSignalId()).isEqualTo(1L);
    assertThat(domain.getAgreementId()).isEqualTo(2L);
    assertThat(domain.getSignalStartDate()).isEqualTo(LocalDate.of(2025, 1, 1));
    assertThat(domain.getSignalEndDate()).isEqualTo(LocalDate.of(2025, 1, 5));
  }
}
