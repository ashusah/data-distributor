package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.AccountBalanceOverviewJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountBalanceOverviewJpaRepository extends JpaRepository<AccountBalanceOverviewJpaEntity, Long> {
}
