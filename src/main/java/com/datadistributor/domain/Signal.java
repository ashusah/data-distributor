package com.datadistributor.domain;

import java.time.LocalDate;
import lombok.Data;

/**
 * Domain model for a signal lifecycle (open/close dates) used in DPD and closure decisions.
 */
@Data
public class Signal {
  private Long signalId;
  private Long agreementId;
  private LocalDate signalStartDate;
  private LocalDate signalEndDate;
}
