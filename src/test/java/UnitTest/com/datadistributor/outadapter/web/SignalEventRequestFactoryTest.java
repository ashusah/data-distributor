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

class SignalEventRequestFactoryTest {

  @Mock
  private InitialCehQueryUseCase initialCehQueryUseCase;
  @Mock
  private AccountBalanceUseCase accountBalanceQueryUseCase;

  private SignalEventRequestFactory factory;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getExternalApi().setBaseUrl("http://api.example");
    properties.getExternalApi().setWriteSignalPath("/create");

    SignalEventPayloadFactory payloadFactory = new SignalEventPayloadFactory(initialCehQueryUseCase, accountBalanceQueryUseCase, properties);
    factory = new SignalEventRequestFactory(properties, payloadFactory);
  }

  @Test
  void buildsRequestWithUriAndPayload() {
    SignalEvent event = new SignalEvent();
    event.setAgreementId(123L);
    event.setSignalId(456L);
    event.setEventRecordDateTime(LocalDateTime.now());
    event.setEventStatus("OVERLIMIT_SIGNAL");
    event.setEventType("CONTRACT_UPDATE");

    when(initialCehQueryUseCase.findInitialCehId(event.getSignalId())).thenReturn(Optional.of("init-1"));
    when(accountBalanceQueryUseCase.findBcNumberByAgreementId(event.getAgreementId())).thenReturn(Optional.of(789L));

    SignalEventRequest request = factory.build(event);

    assertThat(request.uri()).isEqualTo("http://api.example/create");
    assertThat(request.payload()).isNotNull();
    assertThat(request.payload().getAgreementId()).isEqualTo(123L);
    assertThat(request.payload().getCustomerId()).isEqualTo(789L);
    assertThat(request.payload().getInitialEventId()).isEqualTo("init-1");
  }
}
