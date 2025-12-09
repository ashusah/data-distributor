package com.datadistributor.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.application.security.JavaSslContextProvider;
import feign.Client;
import feign.Logger;
import feign.Request;
import feign.Retryer;
import feign.httpclient.ApacheHttpClient;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Tests for Feign configuration defaults.
 */
class FeignConfigurationTest {

  @Mock
  private JavaSslContextProvider javaSslContextProvider;

  private DataDistributorProperties properties;
  private FeignConfiguration config;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
    config = new FeignConfiguration(properties);
  }

  @Test
  void requestOptionsReflectProperties() {
    properties.getHttp().setConnectTimeoutMs(123);
    properties.getHttp().setResponseTimeoutSeconds(9);

    Request.Options options = config.requestOptions();

    assertThat(options.connectTimeoutMillis()).isEqualTo(123);
    assertThat(options.readTimeoutMillis()).isEqualTo(TimeUnit.SECONDS.toMillis(9));
  }

  @Test
  void retryerAndLogLevelConfigured() {
    assertThat(config.retryer()).isSameAs(Retryer.NEVER_RETRY);
    assertThat(config.feignLoggerLevel()).isEqualTo(Logger.Level.BASIC);
  }

  // *****************************
  // FRESH TEST CASE
  // *****************************

  @Test
  void feignClient_usesDefaultClientWhenSslContextNotAvailable() {
    when(javaSslContextProvider.sslContext()).thenReturn(Optional.empty());

    Client client = config.feignClient(javaSslContextProvider);

    assertThat(client).isNotNull();
    assertThat(client).isInstanceOf(ApacheHttpClient.class);
  }

  @Test
  void feignClient_usesSslContextWhenAvailable() throws Exception {
    SSLContext sslContext = SSLContext.getDefault();
    when(javaSslContextProvider.sslContext()).thenReturn(Optional.of(sslContext));

    Client client = config.feignClient(javaSslContextProvider);

    assertThat(client).isNotNull();
    assertThat(client).isInstanceOf(ApacheHttpClient.class);
  }
}
