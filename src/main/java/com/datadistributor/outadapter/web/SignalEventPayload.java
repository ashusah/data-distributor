package com.datadistributor.outadapter.web;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SignalEventPayload {
  private Long agreementId;
  private Long customerId;
  private String initialEventId;
  private String publisher;
  private String publisherId;
  private String status;
  private LocalDateTime submittedDateTime;
  private String type;
}
