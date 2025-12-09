package com.datadistributor.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.application.security.KeyVaultKeyStoreLoader;
import io.netty.handler.ssl.SslContext;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Additional coverage for KeyVaultSslContextProvider without hitting external services.
 */
class KeyVaultSslContextProviderCoverageTest {

  @Test
  void returnsCachedContextWhenPresent() {
    DataDistributorProperties props = new DataDistributorProperties();
    props.getAzure().getKeyvault().setEnabled(true);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(props, mock(KeyVaultKeyStoreLoader.class));
    Optional<SslContext> cached = Optional.of(mock(SslContext.class));
    ReflectionTestUtils.setField(provider, "cachedContext", cached);

    assertThat(provider.sslContext()).isEqualTo(cached);
  }

  @Test
  void enabledButMissingConfigReturnsEmpty() {
    DataDistributorProperties props = new DataDistributorProperties();
    props.getAzure().getKeyvault().setEnabled(true);
    props.getAzure().getKeyvault().setVaultUrl("");
    props.getAzure().getKeyvault().setCertificateName("");
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(props, mock(KeyVaultKeyStoreLoader.class));

    assertThat(provider.sslContext()).isEmpty();
  }
}
