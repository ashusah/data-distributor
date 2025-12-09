package com.datadistributor.application.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import java.util.Optional;
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

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void returnsEmptyWhenVaultUrlIsNull() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl(null);
    properties.getAzure().getKeyvault().setCertificateName("cert");
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void returnsEmptyWhenVaultUrlIsBlank() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("   ");
    properties.getAzure().getKeyvault().setCertificateName("cert");
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void returnsEmptyWhenCertificateNameIsNull() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("https://vault.azure.net");
    properties.getAzure().getKeyvault().setCertificateName(null);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void returnsEmptyWhenCertificateNameIsBlank() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("https://vault.azure.net");
    properties.getAzure().getKeyvault().setCertificateName("   ");
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void cachesResultOnSecondCall() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(false);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    Optional<io.netty.handler.ssl.SslContext> first = provider.sslContext();
    Optional<io.netty.handler.ssl.SslContext> second = provider.sslContext();

    assertThat(first).isEqualTo(second);
  }

  @Test
  void handlesExceptionDuringLoad() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("https://invalid-url");
    properties.getAzure().getKeyvault().setCertificateName("cert");
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    // Should return empty on exception, not throw
    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void handlesNullCertificatePassword() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("https://vault.azure.net");
    properties.getAzure().getKeyvault().setCertificateName("cert");
    properties.getAzure().getKeyvault().setCertificatePassword(null);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    // Should handle null password gracefully
    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void handlesEmptyCertificatePassword() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("https://vault.azure.net");
    properties.getAzure().getKeyvault().setCertificateName("cert");
    properties.getAzure().getKeyvault().setCertificatePassword("");
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    // Should handle empty password gracefully
    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void handlesNullManagedIdentityClientId() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("https://vault.azure.net");
    properties.getAzure().getKeyvault().setCertificateName("cert");
    properties.getAzure().getKeyvault().setClientId(null);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    // Should handle null client ID gracefully
    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void handlesBlankManagedIdentityClientId() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getAzure().getKeyvault().setEnabled(true);
    properties.getAzure().getKeyvault().setVaultUrl("https://vault.azure.net");
    properties.getAzure().getKeyvault().setCertificateName("cert");
    properties.getAzure().getKeyvault().setClientId("   ");
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties);

    // Should handle blank client ID gracefully
    assertThat(provider.sslContext()).isEmpty();
  }
}
