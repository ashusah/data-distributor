package com.datadistributor.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Tests for async executor configuration.
 */
class AsyncConfigurationTest {

  private final AsyncConfiguration config = new AsyncConfiguration();

  @Test
  void dataDistributorExecutorIsConfigured() {
    ThreadPoolTaskExecutor executor = config.dataDistributorTaskExecutor();
    assertThat(executor.getCorePoolSize()).isEqualTo(20);
    assertThat(executor.getMaxPoolSize()).isEqualTo(50);
  }

  @Test
  void rateLimitedExecutorDelegates() {
    ThreadPoolTaskExecutor executor = config.dataDistributorTaskExecutor();
    assertThat(config.rateLimitedExecutor(executor)).isSameAs(executor);
  }
}
