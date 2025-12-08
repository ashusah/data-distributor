package com.datadistributor.outadapter.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * JPA entity mapped to signal_events representing individual status updates for a signal.
 */
@Entity
@Table(name = "signal_events")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SignalEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "uabs_event_id")
    @EqualsAndHashCode.Include
    private Long uabsEventId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signal_id", referencedColumnName = "signal_id", nullable = false)
    private SignalJpaEntity signal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", referencedColumnName = "agreement_id", nullable = false)
    private AccountBalanceJpaEntity accountBalance;

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

    @ManyToOne(cascade = CascadeType.MERGE)
    @JoinColumn(name = "grv", referencedColumnName = "grv", nullable = false)
    private ProductRiskMonitoringJpaEntity grv;

    @Column(name = "product_id")
    private Short productId;
}
