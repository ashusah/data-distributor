package com.datadistributor.application.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Aggregates JPA entity scan and repository configuration.
 */
@Configuration
@EnableTransactionManagement
@EnableJpaRepositories(basePackages = "com.datadistributor.outadapter.repository.springjpa")
@EntityScan(basePackages = "com.datadistributor.outadapter.entity")
public class PersistenceConfiguration {
}
