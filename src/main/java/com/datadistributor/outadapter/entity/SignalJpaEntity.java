package com.datadistributor.outadapter.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import lombok.Data;

/**
 * JPA entity representing the core signal lifecycle record.
 */
@Entity
@Table(name = "signal")
@Data
public class SignalJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "signal_id")
  private Long signalId;

  @Column(name = "agreement_id")
  private Long agreementId;

  @Column(name = "signal_start_date")
  private LocalDate signalStartDate;

  @Column(name = "signal_end_date")
  private LocalDate signalEndDate;
}
