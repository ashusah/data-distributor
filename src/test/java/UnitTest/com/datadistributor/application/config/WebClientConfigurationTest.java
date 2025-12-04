package com.datadistributor.application.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.datadistributor.application.security.SslContextProvider;
import io.netty.handler.ssl.SslContext;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Tests WebClient configuration for both SSL and non-SSL paths.
 */
class WebClientConfigurationTest {

  @Mock
  private SslContextProvider sslContextProvider;

  private DataDistributorProperties properties;
  private WebClientConfiguration config;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
    config = new WebClientConfiguration();
  }

  @Test
  void buildsClientWithoutSsl() {
    when(sslContextProvider.sslContext()).thenReturn(Optional.empty());

    WebClient client = config.webClient(sslContextProvider, properties);

    assertThat(client).isNotNull();
  }

  @Test
  void buildsClientWithSsl() {
    when(sslContextProvider.sslContext()).thenReturn(Optional.of(org.mockito.Mockito.mock(SslContext.class)));

    WebClient client = config.webClient(sslContextProvider, properties);

    assertThat(client).isNotNull();
  }
}
