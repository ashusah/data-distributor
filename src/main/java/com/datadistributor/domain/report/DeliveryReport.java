package com.datadistributor.domain.report;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

/**
 * Domain representation of a delivery status report for a given processing date.
 */
@Value
@Builder
public class DeliveryReport {
  LocalDate date;
  long totalEvents;
  long successEvents;
  long failedEvents;
  String content;
}
