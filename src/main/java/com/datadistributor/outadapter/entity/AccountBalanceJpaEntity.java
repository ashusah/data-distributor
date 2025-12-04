package com.datadistributor.outadapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

  @Column(name = "grv", nullable = false)
  private Short grv;

  @Column(name = "iban", nullable = false, length = 18)
  private String iban;

  @Column(name = "life_cycle_status", nullable = false)
  private Byte lifeCycleStatus;

  @Column(name = "bc_number", nullable = false)
  private Long bcNumber;

  @Column(name = "currency_code", nullable = false, length = 3)
  private String currencyCode;

  @Column(name = "book_date", nullable = false)
  private LocalDate bookDate;

  @Column(name = "unauthorized_debit_balance", nullable = false)
  private Long unauthorizedDebitBalance;

  @Column(name = "last_book_date_balance_cr_to_dt", nullable = false)
  private LocalDate lastBookDateBalanceCrToDt;

  @Column(name = "is_agreement_part_of_acbs", nullable = false, length = 1)
  private String isAgreementPartOfAcbs;

  @Column(name = "is_margin_account_linked", nullable = false, length = 1)
  private String isMarginAccountLinked = "N";

  @Column(name = "product_id")
  private String productId;
}
