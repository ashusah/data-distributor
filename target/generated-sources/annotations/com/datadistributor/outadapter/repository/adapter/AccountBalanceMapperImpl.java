package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.AccountBalance;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import com.datadistributor.outadapter.entity.ProductRiskMonitoringJpaEntity;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2025-12-04T16:22:48+0100",
    comments = "version: 1.5.5.Final, compiler: javac, environment: Java 17.0.17 (Ubuntu)"
)
@Component
public class AccountBalanceMapperImpl implements AccountBalanceMapper {

    @Override
    public AccountBalance toDomain(AccountBalanceJpaEntity entity) {
        if ( entity == null ) {
            return null;
        }

        AccountBalance accountBalance = new AccountBalance();

        accountBalance.setAgreementId( entity.getAgreementId() );
        accountBalance.setUnauthorizedDebitBalance( entity.getUnauthorizedDebitBalance() );
        accountBalance.setGrv( entityGrvGrv( entity ) );
        Short productId = entityGrvProductId( entity );
        if ( productId != null ) {
            accountBalance.setProductId( String.valueOf( productId ) );
        }
        accountBalance.setCurrencyCode( entity.getCurrencyCode() );
        accountBalance.setIban( entity.getIban() );
        accountBalance.setBcNumber( entity.getBcNumber() );
        accountBalance.setLastBookDateBalanceCrToDt( entity.getLastBookDateBalanceCrToDt() );
        accountBalance.setBookDate( entity.getBookDate() );

        return accountBalance;
    }

    @Override
    public List<AccountBalance> toDomainList(List<AccountBalanceJpaEntity> entities) {
        if ( entities == null ) {
            return null;
        }

        List<AccountBalance> list = new ArrayList<AccountBalance>( entities.size() );
        for ( AccountBalanceJpaEntity accountBalanceJpaEntity : entities ) {
            list.add( toDomain( accountBalanceJpaEntity ) );
        }

        return list;
    }

    private Short entityGrvGrv(AccountBalanceJpaEntity accountBalanceJpaEntity) {
        if ( accountBalanceJpaEntity == null ) {
            return null;
        }
        ProductRiskMonitoringJpaEntity grv = accountBalanceJpaEntity.getGrv();
        if ( grv == null ) {
            return null;
        }
        Short grv1 = grv.getGrv();
        if ( grv1 == null ) {
            return null;
        }
        return grv1;
    }

    private Short entityGrvProductId(AccountBalanceJpaEntity accountBalanceJpaEntity) {
        if ( accountBalanceJpaEntity == null ) {
            return null;
        }
        ProductRiskMonitoringJpaEntity grv = accountBalanceJpaEntity.getGrv();
        if ( grv == null ) {
            return null;
        }
        Short productId = grv.getProductId();
        if ( productId == null ) {
            return null;
        }
        return productId;
    }
}
