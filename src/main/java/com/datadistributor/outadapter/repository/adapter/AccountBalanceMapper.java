package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.AccountBalance;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(
    componentModel = "spring",
    unmappedTargetPolicy = ReportingPolicy.IGNORE
)
public interface AccountBalanceMapper {

    @Mapping(target = "agreementId", source = "agreementId")
    @Mapping(target = "unauthorizedDebitBalance", source = "unauthorizedDebitBalance")
    @Mapping(target = "grv", source = "grv")
    @Mapping(target = "productId", source = "productId")
    @Mapping(target = "currencyCode", source = "currencyCode")
    @Mapping(target = "iban", source = "iban")
    @Mapping(target = "bcNumber", source = "bcNumber")
    @Mapping(target = "lastBookDateBalanceCrToDt", source = "lastBookDateBalanceCrToDt")
    @Mapping(target = "bookDate", source = "bookDate")
    AccountBalance toDomain(AccountBalanceJpaEntity entity);

    List<AccountBalance> toDomainList(List<AccountBalanceJpaEntity> entities);
}
