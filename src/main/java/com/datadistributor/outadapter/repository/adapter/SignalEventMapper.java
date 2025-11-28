package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class SignalEventMapper {

    public SignalEvent toDomain(SignalEventJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        SignalEvent domain = new SignalEvent();
        domain.setUabsEventId(entity.getUabsEventId());
        domain.setSignalId(entity.getSignalId());
        domain.setAgreementId(entity.getAgreementId());
        domain.setEventRecordDateTime(entity.getEventRecordDateTime());
        domain.setEventType(entity.getEventType());
        domain.setEventStatus(entity.getEventStatus());
        domain.setUnauthorizedDebitBalance(entity.getUnauthorizedDebitBalance());
        domain.setBookDate(entity.getBookDate());
        domain.setGrv(entity.getGrv());
        domain.setProductId(entity.getProductId());

        return domain;
    }

    public List<SignalEvent> toDomain(List<SignalEventJpaEntity> entities) {
        if (entities == null) {
            return List.of();
        }

        return entities.stream()
                .filter(Objects::nonNull)
                .map(this::toDomain)
                .collect(Collectors.toList());
    }
}
