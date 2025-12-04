package com.datadistributor.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

/**
 * Tests for Feign configuration defaults.
 */
class FeignConfigurationTest {

  @Test
  void requestOptionsReflectProperties() {
    DataDistributorProperties props = new DataDistributorProperties();
    props.getHttp().setConnectTimeoutMs(123);
    props.getHttp().setResponseTimeoutSeconds(9);
    FeignConfiguration config = new FeignConfiguration(props);

    Request.Options options = config.requestOptions();

    assertThat(options.connectTimeoutMillis()).isEqualTo(123);
    assertThat(options.readTimeoutMillis()).isEqualTo(TimeUnit.SECONDS.toMillis(9));
  }

  @Test
  void retryerAndLogLevelConfigured() {
    FeignConfiguration config = new FeignConfiguration(new DataDistributorProperties());
    assertThat(config.retryer()).isSameAs(Retryer.NEVER_RETRY);
    assertThat(config.feignLoggerLevel()).isEqualTo(Logger.Level.BASIC);
  }
}
