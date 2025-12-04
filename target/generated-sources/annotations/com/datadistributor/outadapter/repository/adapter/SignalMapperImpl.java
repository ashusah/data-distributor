package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.Signal;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
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
public class SignalMapperImpl implements SignalMapper {

    @Override
    public Signal toDomain(SignalJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        Signal signal = new Signal();

        signal.setSignalId( entity.getSignalId() );
        signal.setAgreementId( entity.getAgreementId() );
        signal.setSignalStartDate( entity.getSignalStartDate() );
        signal.setSignalEndDate( entity.getSignalEndDate() );

        return signal;
    }

    @Override
    public List<Signal> toDomainList(List<SignalJpaEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<Signal> list = new ArrayList<Signal>( entities.size() );
        for ( SignalJpaEntity signalJpaEntity : entities ) {
            list.add( toDomain( signalJpaEntity ) );
        }

        return list;
    }
}
