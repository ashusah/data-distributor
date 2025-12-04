package com.datadistributor.application.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Defines the shared async executors used by schedulers and outbound calls.
 */
@Configuration
@EnableAsync
@RequiredArgsConstructor
public class AsyncConfiguration {

  private final DataDistributorProperties properties;

  @Bean(name = "dataDistributorTaskExecutor")
  @Primary
  public ThreadPoolTaskExecutor dataDistributorTaskExecutor() {
    DataDistributorProperties.Async async = properties.getAsync();
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(async.getCorePoolSize());
    executor.setMaxPoolSize(async.getMaxPoolSize());
    executor.setQueueCapacity(async.getQueueCapacity());
    executor.setThreadNamePrefix(async.getThreadNamePrefix());
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    return executor;
  }

  @Bean
  public Executor rateLimitedExecutor(@Qualifier("dataDistributorTaskExecutor") TaskExecutor dataDistributorTaskExecutor) {
    return dataDistributorTaskExecutor;
  }
}
