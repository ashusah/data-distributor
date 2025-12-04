package com.datadistributor.outadapter.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * Represents the product risk monitoring configuration keyed by GRV/product, used to enrich
 * account balance records.
 */
@Data
@Entity
@Table(name = "product_risk_monitoring")
public class ProductRiskMonitoringJpaEntity {

    @Id
    @Column(name = "grv", nullable = false, updatable = false)
    private Short grv;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Short productId;

    @Column(name = "currency_code", nullable = false, length = 3, columnDefinition = "CHAR(3)")
    private String currencyCode;

    @Column(name = "monitor_kraandicht", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String monitorKraandicht;

    @Column(name = "monitor_CW014_signal", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String monitorCW014Signal;

    @Column(name = "report_CW014_to_ceh", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String reportCW014ToCEH;

    @Column(name = "report_CW014_to_dial", nullable = false, length = 1, columnDefinition = "CHAR(1)")
    private String reportCW014ToDial;
}
