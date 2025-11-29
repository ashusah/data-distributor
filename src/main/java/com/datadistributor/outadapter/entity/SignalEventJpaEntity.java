package com.datadistributor.outadapter.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "signals")
@Data
public class SignalEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uabs_event_id")
    private Long uabsEventId;

    @Column(name = "signalId")
    private Long signalId;

    @Column(name = "agreement_id")
    private Long agreementId;

    @Column(name = "event_record_date_time")
    private LocalDateTime eventRecordDateTime;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_status")
    private String eventStatus;

    @Column(name = "unauthorized_debit_balance")
    private Long unauthorizedDebitBalance;

    @Column(name = "book_date")
    private LocalDate bookDate;

    @Column(name = "grv")
    private Short grv;

    @Column(name = "product_id")
    private Short productId;
}

