package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.SignalEvent;
import com.datadistributor.outadapter.entity.SignalEventJpaEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SignalEventMapper {

    @Mapping(source = "signalId", target = "signalId")
    @Mapping(source = "agreementId", target = "agreementId")
    SignalEvent toDomain(SignalEventJpaEntity entity);

    List<SignalEvent> toDomainList(List<SignalEventJpaEntity> entities);
}
