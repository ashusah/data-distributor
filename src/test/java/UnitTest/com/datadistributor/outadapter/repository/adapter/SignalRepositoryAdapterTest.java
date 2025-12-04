package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.outadapter.entity.SignalJpaEntity;
import com.datadistributor.outadapter.repository.springjpa.SignalJpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.mapstruct.factory.Mappers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SignalRepositoryAdapterTest {

  @Mock
  private SignalJpaRepository repository;

  private SignalRepositoryAdapter adapter;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    adapter = new SignalRepositoryAdapter(repository, Mappers.getMapper(SignalMapper.class));
  }

  @Test
  void findBySignalId_returnsEmptyForNull() {
    assertThat(adapter.findBySignalId(null)).isEmpty();
  }

  @Test
  void findBySignalId_mapsEntity() {
    SignalJpaEntity entity = new SignalJpaEntity();
    entity.setSignalId(1L);
    when(repository.findById(1L)).thenReturn(Optional.of(entity));

    assertThat(adapter.findBySignalId(1L)).get().extracting("signalId").isEqualTo(1L);
  }

  @Test
  void findByAgreementId_returnsEmptyForNull() {
    assertThat(adapter.findByAgreementId(null)).isEmpty();
  }

  @Test
  void findByAgreementId_mapsEntity() {
    SignalJpaEntity entity = new SignalJpaEntity();
    entity.setSignalId(5L);
    when(repository.findByAgreementId(2L)).thenReturn(Optional.of(entity));

    assertThat(adapter.findByAgreementId(2L)).get().extracting("signalId").isEqualTo(5L);
  }

  @Test
  void findByStartDateBefore_returnsEmptyOnNullDate() {
    assertThat(adapter.findByStartDateBefore(null)).isEmpty();
  }

  @Test
  void findByStartDateBefore_mapsAll() {
    SignalJpaEntity entity = new SignalJpaEntity();
    entity.setSignalId(8L);
    when(repository.findBySignalStartDateLessThanEqual(LocalDate.of(2024, 1, 1)))
        .thenReturn(List.of(entity));

    assertThat(adapter.findByStartDateBefore(LocalDate.of(2024, 1, 1)))
        .singleElement()
        .extracting("signalId")
        .isEqualTo(8L);
  }
}
