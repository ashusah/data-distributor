package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.AccountBalanceOverviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for account balance overview rows keyed by agreement id.
 */
@Repository
public interface AccountBalanceOverviewJpaRepository extends JpaRepository<AccountBalanceOverviewJpaEntity, Long> {
}
