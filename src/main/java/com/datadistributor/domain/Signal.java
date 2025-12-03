package com.datadistributor.domain;

import java.time.LocalDate;
import lombok.Data;

@Data
public class Signal {
  private Long signalId;
  private Long agreementId;
  private LocalDate signalStartDate;
  private LocalDate signalEndDate;
}
