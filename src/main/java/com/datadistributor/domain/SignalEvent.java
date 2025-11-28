package com.datadistributor.domain;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode
public class SignalEvent {
    private Long uabsEventId;
    private Long signalId;
    private Long agreementId;
    private LocalDateTime eventRecordDateTime;
    private String eventType;
    private String eventStatus;
    private Long unauthorizedDebitBalance;
    private LocalDate bookDate;
    private Short grv;
    private Short productId;
}

