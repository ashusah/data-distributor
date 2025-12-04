package com.datadistributor.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import org.junit.jupiter.api.Test;

/**
 * Verifies KeyVault SSL provider behavior when disabled.
 */
class KeyVaultSslContextProviderTest {

  @Test
  void returnsEmptyWhenDisabled() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(false);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    assertThat(provider.sslContext()).isEmpty();
    // cached result stays empty
    assertThat(provider.sslContext()).isEmpty();
  }
}
