package com.datadistributor.domain;

import java.time.LocalDate;
import lombok.Data;

/**
 * Domain projection of account balance details enriched for DIAL exports and signal context.
 */
@Data
public class AccountBalance {
  private Long agreementId;
  private Short grv;
  private String iban;
  private Long bcNumber;
  private String currencyCode;
  private LocalDate bookDate;
  private String productId;
  private Long unauthorizedDebitBalance;
  private LocalDate lastBookDateBalanceCrToDt;
}
