package com.datadistributor.outadapter.repository.springjpa;

import com.datadistributor.outadapter.entity.AccountBalanceJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data repository for account balance rows keyed by agreement id.
 */
@Repository
public interface AccountBalanceJpaRepository extends JpaRepository<AccountBalanceJpaEntity, Long> {
}
