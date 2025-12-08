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

    @Mapping(source = "signal.signalId", target = "signalId")
    @Mapping(source = "accountBalance.agreementId", target = "agreementId")
    @Mapping(target = "grv", source = "grv.grv")
    SignalEvent toDomain(SignalEventJpaEntity entity);

    List<SignalEvent> toDomainList(List<SignalEventJpaEntity> entities);
}
