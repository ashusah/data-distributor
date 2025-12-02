package com.datadistributor.outadapter.web;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.domain.inport.AccountBalanceQueryUseCase;
import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SignalEventPayloadFactory {

  private final InitialCehQueryUseCase initialCehQueryUseCase;
  private final AccountBalanceQueryUseCase accountBalanceQueryUseCase;
  @Value("${data-distributor.external-api.publisher:UABS}")
  private String publisher;
  @Value("${data-distributor.external-api.publisher-id:0bfe5670-457d-4872-a1f1-efe4db39f099}")
  private String publisherId;

  public SignalEventPayload buildPayload(SignalEvent event) {
    String initialEventId = initialCehQueryUseCase.findInitialCehId(event.getSignalId()).orElse(null);
    Long customerId = accountBalanceQueryUseCase
        .findBcNumberByAgreementId(event.getAgreementId())
        .orElse(null);

    return new SignalEventPayload(
        event.getAgreementId(),
        customerId,
        initialEventId,
        publisher,
        publisherId,
        event.getEventStatus(),
        event.getEventRecordDateTime(),
        event.getEventType()
    );
  }
}
