package com.datadistributor.outadapter.repository.springjpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Answers.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;
import static org.mockito.Mockito.when;

import com.datadistributor.outadapter.entity.CehResponseInitialEventEntity;
import com.datadistributor.outadapter.entity.CehResponseInitialEventId;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CehResponseInitialEventRepositoryTest {

  private CehResponseInitialEventRepository repository;

  @BeforeEach
  void setup() {
    repository = mock(CehResponseInitialEventRepository.class,
        withSettings().defaultAnswer(CALLS_REAL_METHODS));
  }

  @Test
  void findFirstByIdSignalId_returnsEmptyWhenNoEntries() {
    when(repository.findByIdSignalId(5L)).thenReturn(List.of());

    assertThat(repository.findFirstByIdSignalId(5L)).isEmpty();
  }

  @Test
  void findFirstByIdSignalId_returnsFirstMatchingEntity() {
    CehResponseInitialEventEntity entity = new CehResponseInitialEventEntity(
        new CehResponseInitialEventId("1", 5L));
    when(repository.findByIdSignalId(5L)).thenReturn(List.of(entity));

    assertThat(repository.findFirstByIdSignalId(5L)).contains(entity);
  }

  // **********************************************************
  // ADDITIONAL TEST
  // **********************************************************

  @Test
  void findFirstByIdSignalId_returnsFirstEntryWhenMultipleResults() {
    CehResponseInitialEventEntity first = new CehResponseInitialEventEntity(
        new CehResponseInitialEventId("1", 5L));
    CehResponseInitialEventEntity second = new CehResponseInitialEventEntity(
        new CehResponseInitialEventId("2", 5L));

    when(repository.findByIdSignalId(5L)).thenReturn(List.of(first, second));

    assertThat(repository.findFirstByIdSignalId(5L)).contains(first);
  }
}
