package com.datadistributor.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Tests for async executor configuration.
 */
class AsyncConfigurationTest {

  private final DataDistributorProperties properties = buildProperties();
  private final AsyncConfiguration config = new AsyncConfiguration(properties);

  @Test
  void dataDistributorExecutorIsConfigured() {
    ThreadPoolTaskExecutor executor = config.dataDistributorTaskExecutor();
    assertThat(executor.getCorePoolSize()).isEqualTo(properties.getAsync().getCorePoolSize());
    assertThat(executor.getMaxPoolSize()).isEqualTo(properties.getAsync().getMaxPoolSize());
    assertThat(executor.getThreadNamePrefix()).isEqualTo(properties.getAsync().getThreadNamePrefix());
  }

  @Test
  void rateLimitedExecutorDelegates() {
    ThreadPoolTaskExecutor executor = config.dataDistributorTaskExecutor();
    assertThat(config.rateLimitedExecutor(executor)).isSameAs(executor);
  }

  private DataDistributorProperties buildProperties() {
    DataDistributorProperties props = new DataDistributorProperties();
    DataDistributorProperties.Async async = new DataDistributorProperties.Async();
    async.setCorePoolSize(4);
    async.setMaxPoolSize(8);
    async.setQueueCapacity(123);
    async.setThreadNamePrefix("TestAsync-");
    props.setAsync(async);
    return props;
  }
}
