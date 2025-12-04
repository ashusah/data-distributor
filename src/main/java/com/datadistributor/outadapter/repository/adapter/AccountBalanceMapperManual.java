package com.datadistributor.outadapter.repository.adapter;

import com.datadistributor.domain.AccountBalance;
import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Manual mapper used in tests/runtime to avoid component-scan quirks with generated mappers.
 * It mirrors the MapStruct mapping so downstream beans can be wired reliably.
 */
@Component
@Primary
public class AccountBalanceMapperManual implements AccountBalanceMapper {

  @Override
  public AccountBalance toDomain(AccountBalanceJpaEntity entity) {
    if (entity == null) {
      return null;
    }
    AccountBalance accountBalance = new AccountBalance();
    accountBalance.setAgreementId(entity.getAgreementId());
    accountBalance.setUnauthorizedDebitBalance(entity.getUnauthorizedDebitBalance());
    accountBalance.setGrv(entity.getGrv());
    accountBalance.setProductId(entity.getProductId());
    accountBalance.setCurrencyCode(entity.getCurrencyCode());
    accountBalance.setIban(entity.getIban());
    accountBalance.setBcNumber(entity.getBcNumber());
    accountBalance.setLastBookDateBalanceCrToDt(entity.getLastBookDateBalanceCrToDt());
    accountBalance.setBookDate(entity.getBookDate());
    return accountBalance;
  }

  @Override
  public List<AccountBalance> toDomainList(List<AccountBalanceJpaEntity> entities) {
    if (entities == null) {
      return Collections.emptyList();
    }
    return entities.stream()
        .filter(Objects::nonNull)
        .map(this::toDomain)
        .collect(Collectors.toList());
  }
}
