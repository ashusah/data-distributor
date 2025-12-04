package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.Signal;
import com.datadistributor.outadapter.entity.SignalJpaEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface SignalMapper {

    Signal toDomain(SignalJpaEntity entity);

    List<Signal> toDomainList(List<SignalJpaEntity> entities);
}
