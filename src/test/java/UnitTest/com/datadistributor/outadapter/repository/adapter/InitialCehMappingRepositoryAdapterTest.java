package com.datadistributor.outadapter.repository.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.outadapter.entity.CehResponseInitialEventEntity;
import com.datadistributor.outadapter.entity.CehResponseInitialEventId;
import com.datadistributor.outadapter.repository.springjpa.CehResponseInitialEventRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class InitialCehMappingRepositoryAdapterTest {

  @Mock
  private CehResponseInitialEventRepository repository;

  private InitialCehMappingRepositoryAdapter adapter;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    adapter = new InitialCehMappingRepositoryAdapter(repository);
  }

  @Test
  void saveInitialCehMapping_persistsCompositeKey() {
    adapter.saveInitialCehMapping(5L, 123L);

    ArgumentCaptor<CehResponseInitialEventEntity> captor = ArgumentCaptor.forClass(CehResponseInitialEventEntity.class);
    verify(repository).save(captor.capture());
    CehResponseInitialEventEntity saved = captor.getValue();
    assertThat(saved.getId().getSignalId()).isEqualTo(5L);
    assertThat(saved.getId().getCehInitialEventId()).isEqualTo("123");
  }

  @Test
  void findInitialCehId_returnsValue() {
    CehResponseInitialEventId id = new CehResponseInitialEventId("456", 9L);
    when(repository.findFirstByIdSignalId(9L)).thenReturn(Optional.of(new CehResponseInitialEventEntity(id)));

    assertThat(adapter.findInitialCehId(9L)).contains("456");
  }

  @Test
  void findInitialCehId_emptyWhenMissing() {
    when(repository.findFirstByIdSignalId(any())).thenReturn(Optional.empty());

    assertThat(adapter.findInitialCehId(10L)).isEmpty();
  }

  // ***************************************************
  // NEW TEST- Date- Dec 9
  // ***************************************************

  @Test
  void saveInitialCehMapping_deletesAndRecreatesWhenExistingHasDifferentCehId() {
    CehResponseInitialEventId existingId = new CehResponseInitialEventId("999", 5L);
    CehResponseInitialEventEntity existing = new CehResponseInitialEventEntity(existingId);
    when(repository.findByIdSignalId(5L)).thenReturn(List.of(existing));

    adapter.saveInitialCehMapping(5L, 123L);

    verify(repository).deleteAll(List.of(existing));
    ArgumentCaptor<CehResponseInitialEventEntity> captor = ArgumentCaptor.forClass(CehResponseInitialEventEntity.class);
    verify(repository).save(captor.capture());
    CehResponseInitialEventEntity saved = captor.getValue();
    assertThat(saved.getId().getSignalId()).isEqualTo(5L);
    assertThat(saved.getId().getCehInitialEventId()).isEqualTo("123");
  }

  @Test
  void saveInitialCehMapping_skipsWhenExistingHasSameCehId() {
    CehResponseInitialEventId existingId = new CehResponseInitialEventId("123", 5L);
    CehResponseInitialEventEntity existing = new CehResponseInitialEventEntity(existingId);
    when(repository.findByIdSignalId(5L)).thenReturn(List.of(existing));

    adapter.saveInitialCehMapping(5L, 123L);

    verify(repository, never()).deleteAll(any());
    verify(repository, never()).save(any());
  }

  // **********************************************************
  // ADDITIONAL TEST
  // **********************************************************

  @Test
  void saveInitialCehMapping_handlesEmptyExistingListExplicitly() {
    when(repository.findByIdSignalId(5L)).thenReturn(List.of());

    adapter.saveInitialCehMapping(5L, 123L);

    ArgumentCaptor<CehResponseInitialEventEntity> captor = ArgumentCaptor.forClass(CehResponseInitialEventEntity.class);
    verify(repository).save(captor.capture());
    verify(repository, never()).deleteAll(any());
    assertThat(captor.getValue().getId().getSignalId()).isEqualTo(5L);
    assertThat(captor.getValue().getId().getCehInitialEventId()).isEqualTo("123");
  }
}
