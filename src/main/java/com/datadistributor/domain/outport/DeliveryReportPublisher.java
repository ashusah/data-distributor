package com.datadistributor.domain.outport;

import com.datadistributor.domain.report.DeliveryReport;

public interface DeliveryReportPublisher {
  void publish(DeliveryReport report);
}
