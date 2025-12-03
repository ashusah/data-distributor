package com.datadistributor.outadapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Data;

@Entity
@Table(name = "signal")
@Data
public class SignalJpaEntity {

  @Id
  @Column(name = "signal_id")
  private Long signalId;

  @Column(name = "agreement_id")
  private Long agreementId;

  @Column(name = "signal_start_date")
  private LocalDate signalStartDate;

  @Column(name = "signal_end_date")
  private LocalDate signalEndDate;
}
