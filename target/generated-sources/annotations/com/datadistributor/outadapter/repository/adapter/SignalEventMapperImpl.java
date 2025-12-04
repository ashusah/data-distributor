package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T16:22:49+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.17 (Ubuntu)"
)
@Component
public class SignalEventMapperImpl implements SignalEventMapper {

    @Override
    public SignalEvent toDomain(SignalEventJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        SignalEvent signalEvent = new SignalEvent();

        signalEvent.setSignalId( entity.getSignalId() );
        signalEvent.setAgreementId( entity.getAgreementId() );
        signalEvent.setUabsEventId( entity.getUabsEventId() );
        signalEvent.setEventRecordDateTime( entity.getEventRecordDateTime() );
        signalEvent.setEventType( entity.getEventType() );
        signalEvent.setEventStatus( entity.getEventStatus() );
        signalEvent.setUnauthorizedDebitBalance( entity.getUnauthorizedDebitBalance() );
        signalEvent.setBookDate( entity.getBookDate() );
        signalEvent.setGrv( entity.getGrv() );
        signalEvent.setProductId( entity.getProductId() );

        return signalEvent;
    }

    @Override
    public List<SignalEvent> toDomainList(List<SignalEventJpaEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<SignalEvent> list = new ArrayList<SignalEvent>( entities.size() );
        for ( SignalEventJpaEntity signalEventJpaEntity : entities ) {
            list.add( toDomain( signalEventJpaEntity ) );
        }

        return list;
    }
}
