package com.datadistributor.outadapter.web;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Payload sent to CEH describing a single signal event.
 */
@Data
@AllArgsConstructor
public class SignalEventPayload {
  private Long agreementId;
  private Long customerId;
  private String initialEventId;
  private String publisher;
  private String publisherId;
  private String status;
  private String eventRecordDateTime;
  private String type;
}
