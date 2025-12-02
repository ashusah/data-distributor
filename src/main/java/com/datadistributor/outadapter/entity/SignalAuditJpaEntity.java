package com.datadistributor.outadapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "signal_audit")
@Data
public class SignalAuditJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "audit_id")
  private Long auditId;

  @Column(name = "signal_id", nullable = false)
  private Long signalId;

  @Column(name = "uabs_event_id", nullable = false)
  private Long uabsEventId;

  @Column(name = "consumer_id", nullable = false)
  private Long consumerId;

  @Column(name = "agreement_id", nullable = false)
  private Long agreementId;

  @Column(name = "unauthorized_debit_balance", nullable = false)
  private Long unauthorizedDebitBalance;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  @Column(name = "response_code", nullable = false, length = 10)
  private String responseCode;

  @Column(name = "response_message", nullable = false, length = 100)
  private String responseMessage;

  @Column(name = "audit_record_date_time", nullable = false)
  private LocalDateTime auditRecordDateTime;
}
