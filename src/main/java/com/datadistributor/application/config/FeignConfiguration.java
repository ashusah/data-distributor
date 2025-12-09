package com.datadistributor.application.config;

import com.datadistributor.application.security.JavaSslContextProvider;
import feign.Client;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.httpclient.ApacheHttpClient;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Centralizes Feign setup (timeouts, logging, retry strategy, SSL) for blocking dispatch mode.
 * SSL context is only applied when Key Vault is enabled (Azure deployment).
 */
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

  /**
   * Configures Feign client with optional SSL context from Azure Key Vault.
   * When Key Vault is disabled (local), uses default SSL context.
   * When Key Vault is enabled (Azure), uses certificate from Key Vault.
   */
  @Bean
  public Client feignClient(JavaSslContextProvider javaSslContextProvider) {
    return javaSslContextProvider.sslContext()
        .map(sslContext -> {
          // Use custom SSL context when Key Vault is enabled
          CloseableHttpClient httpClient = HttpClients.custom()
              .setSSLContext(sslContext)
              .build();
          return new ApacheHttpClient(httpClient);
        })
        .orElse(new ApacheHttpClient()); // Default client when Key Vault is disabled (local)
  }
}
