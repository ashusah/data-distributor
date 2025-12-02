package com.datadistributor.application.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.openfeign.EnableFeignClients;

@Configuration
@EnableFeignClients(basePackages = "com.datadistributor.outadapter.web")
public class FeignConfiguration {

  private final DataDistributorProperties properties;

  public FeignConfiguration(DataDistributorProperties properties) {
    this.properties = properties;
  }

  @Bean
  public Request.Options requestOptions() {
    var http = properties.getHttp();
    return new Request.Options(
        http.getConnectTimeoutMs(), TimeUnit.MILLISECONDS,
        TimeUnit.SECONDS.toMillis(http.getResponseTimeoutSeconds()), TimeUnit.MILLISECONDS,
        true
    );
  }

  @Bean
  public Retryer retryer() {
    return Retryer.NEVER_RETRY;
  }

  @Bean
  public Logger.Level feignLoggerLevel() {
    return Logger.Level.BASIC;
  }
}
