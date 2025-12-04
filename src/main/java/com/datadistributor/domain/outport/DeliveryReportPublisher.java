package com.datadistributor.domain.outport;

import com.datadistributor.domain.report.DeliveryReport;

/**
 * Publishes delivery reports (e.g., to storage).
 */
public interface DeliveryReportPublisher {
  void publish(DeliveryReport report);
}
