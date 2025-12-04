package com.datadistributor.outadapter.web;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.inport.AccountBalanceUseCase;
import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Produces payloads enriched with customer and initial CEH ids for outbound dispatch.
 */
@Component
@RequiredArgsConstructor
public class SignalEventPayloadFactory {

  private final InitialCehQueryUseCase initialCehQueryUseCase;
  private final AccountBalanceUseCase accountBalanceQueryUseCase;
  private final DataDistributorProperties properties;

  public SignalEventPayload buildPayload(SignalEvent event) {
    String initialEventId = initialCehQueryUseCase.findInitialCehId(event.getSignalId()).orElse(null);
    Long customerId = accountBalanceQueryUseCase
        .findBcNumberByAgreementId(event.getAgreementId())
        .orElse(null);

    return new SignalEventPayload(
        event.getAgreementId(),
        customerId,
        initialEventId,
        properties.getExternalApi().getPublisher(),
        properties.getExternalApi().getPublisherId(),
        event.getEventStatus(),
        event.getEventRecordDateTime(),
        event.getEventType()
    );
  }
}
