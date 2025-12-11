package com.datadistributor.outadapter.web;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.inport.AccountBalanceUseCase;
import com.datadistributor.domain.inport.InitialCehQueryUseCase;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Produces payloads enriched with customer and initial CEH ids for outbound dispatch.
 */
@Component
@RequiredArgsConstructor
public class SignalEventPayloadFactory {

  private static final DateTimeFormatter ISO_INSTANT = DateTimeFormatter.ISO_INSTANT;

  private final InitialCehQueryUseCase initialCehQueryUseCase;
  private final AccountBalanceUseCase accountBalanceQueryUseCase;
  private final DataDistributorProperties properties;

  public SignalEventPayload buildPayload(SignalEvent event) {
    String initialEventId = initialCehQueryUseCase.findInitialCehId(event.getSignalId()).orElse(null);
    Long customerId = accountBalanceQueryUseCase
        .findBcNumberByAgreementId(event.getAgreementId())
        .orElse(null);

    String submittedDateTime = formatEventRecordDateTime(event.getEventRecordDateTime());

    return new SignalEventPayload(
        event.getAgreementId(),
        customerId,
        initialEventId,
        properties.getExternalApi().getPublisher(),
        properties.getExternalApi().getPublisherId(),
        event.getEventStatus(),
        submittedDateTime,
        event.getEventType()
    );
  }

  private String formatEventRecordDateTime(java.time.LocalDateTime dateTime) {
    if (dateTime == null) {
      return null;
    }
    return dateTime.atOffset(ZoneOffset.UTC).format(ISO_INSTANT);
  }
}
