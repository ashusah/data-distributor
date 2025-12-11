package com.datadistributor.outadapter.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.AccountBalanceUseCase;
import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class SignalEventPayloadFactoryAdditionalTest {

  @Mock
  private InitialCehQueryUseCase initialCehQueryUseCase;

  @Mock
  private AccountBalanceUseCase accountBalanceQueryUseCase;

  private SignalEventPayloadFactory factory;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getExternalApi().setPublisher("PUB");
    properties.getExternalApi().setPublisherId("PUB_ID");
    factory = new SignalEventPayloadFactory(initialCehQueryUseCase, accountBalanceQueryUseCase, properties);
  }

  @Test
  void buildPayload_enrichesIdsAndMetadata() {
    SignalEvent event = new SignalEvent();
    event.setAgreementId(10L);
    event.setSignalId(11L);
    event.setEventStatus("OVERLIMIT_SIGNAL");
    event.setEventRecordDateTime(LocalDateTime.of(2024, 1, 1, 0, 0));
    event.setEventType("TYPE");

    when(initialCehQueryUseCase.findInitialCehId(11L)).thenReturn(Optional.of("INIT"));
    when(accountBalanceQueryUseCase.findBcNumberByAgreementId(10L)).thenReturn(Optional.of(555L));

    SignalEventPayload payload = factory.buildPayload(event);

    assertThat(payload.getAgreementId()).isEqualTo(10L);
    assertThat(payload.getCustomerId()).isEqualTo(555L);
    assertThat(payload.getInitialEventId()).isEqualTo("INIT");
    assertThat(payload.getPublisher()).isEqualTo("PUB");
    assertThat(payload.getPublisherId()).isEqualTo("PUB_ID");
    assertThat(payload.getStatus()).isEqualTo("OVERLIMIT_SIGNAL");
    assertThat(payload.getEventRecordDateTime()).isEqualTo("2024-01-01T00:00:00Z");
    assertThat(payload.getType()).isEqualTo("TYPE");
  }
}
