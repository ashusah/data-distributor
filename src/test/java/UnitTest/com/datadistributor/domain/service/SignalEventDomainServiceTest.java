package com.datadistributor.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.outport.SignalEventRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link SignalEventDomainService}.
 */
class SignalEventDomainServiceTest {

  @Mock
  private SignalEventRepository repository;

  private SignalEventDomainService service;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    service = new SignalEventDomainService(repository, 2);
  }

  @Test
  void getAllSignalEventsOfThisDate_delegates() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    when(repository.getAllSignalEventsOfThisDate(date)).thenReturn(List.of(new SignalEvent()));

    assertThat(service.getAllSignalEventsOfThisDate(date)).hasSize(1);
  }

  @Test
  void getAllSignalForCEH_pagesUntilEmpty() {
    LocalDate date = LocalDate.of(2024, 12, 3);
    SignalEvent first = new SignalEvent();
    SignalEvent second = new SignalEvent();
    when(repository.getSignalEventsForCEH(date, 0, 2)).thenReturn(List.of(first));
    when(repository.getSignalEventsForCEH(date, 1, 2)).thenReturn(List.of(second));
    when(repository.getSignalEventsForCEH(date, 2, 2)).thenReturn(List.of());

    List<SignalEvent> result = service.getAllSignalForCEH(date);

    assertThat(result).containsExactly(first, second);
  }
}
