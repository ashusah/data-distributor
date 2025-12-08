package com.datadistributor.outadapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.CascadeType;
import java.time.LocalDate;
import lombok.Data;

/**
 * Database representation of the account balance used to enrich exported signal data.
 */
@Entity
@Table(name = "account_balance_overview")
@Data
public class AccountBalanceJpaEntity {

  @Id
  @Column(name = "agreement_id", nullable = false)
  private Long agreementId;

  @ManyToOne(cascade = CascadeType.MERGE)
  @JoinColumn(name = "grv", referencedColumnName = "grv", nullable = false)
  private ProductRiskMonitoringJpaEntity grv;

  @Column(name = "iban", nullable = false, length = 18, columnDefinition = "CHAR(18)")
  private String iban;

  @Column(name = "life_cycle_status", nullable = false)
  private Short lifeCycleStatus;

  @Column(name = "bc_number", nullable = false)
  private Long bcNumber;

  @Column(name = "currency_code", nullable = false, length = 3, columnDefinition = "CHAR(3)")
  private String currencyCode;

  @Column(name = "book_date", nullable = false)
  private LocalDate bookDate;

  @Column(name = "unauthorized_debit_balance", nullable = false)
  private Long unauthorizedDebitBalance;

  @Column(name = "last_book_date_balance_cr_to_dt", nullable = false)
  private LocalDate lastBookDateBalanceCrToDt;

  @Column(name = "is_agreement_part_of_acbs", nullable = false, length = 1)
  private String isAgreementPartOfAcbs;
}
