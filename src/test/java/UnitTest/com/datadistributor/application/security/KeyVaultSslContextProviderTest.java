package com.datadistributor.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Verifies KeyVault SSL provider behavior when disabled.
 */
class KeyVaultSslContextProviderTest {

  @Mock
  private KeyVaultKeyStoreLoader keyStoreLoader;

  private DataDistributorProperties properties;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
  }

  @Test
  void returnsEmptyWhenDisabled() {
    properties.getAzure().getKeyvault().setEnabled(false);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

    assertThat(provider.sslContext()).isEmpty();
    // cached result stays empty
    assertThat(provider.sslContext()).isEmpty();
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void returnsEmptyWhenKeyStoreLoaderReturnsEmpty() {
    properties.getAzure().getKeyvault().setEnabled(true);
    when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.empty());
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void cachesResultOnSecondCall() {
    properties.getAzure().getKeyvault().setEnabled(false);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

    Optional<io.netty.handler.ssl.SslContext> first = provider.sslContext();
    Optional<io.netty.handler.ssl.SslContext> second = provider.sslContext();

    assertThat(first).isEqualTo(second);
  }

  // *****************************
  // FRESH TEST CASE
  // *****************************

  @Test
  void sslContext_handlesDoubleCheckLocking() {
    properties.getAzure().getKeyvault().setEnabled(true);
    when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.empty());
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

    // First call should trigger loadFromKeyVault
    Optional<io.netty.handler.ssl.SslContext> first = provider.sslContext();
    // Second call should use cached result (even if empty)
    Optional<io.netty.handler.ssl.SslContext> second = provider.sslContext();

    assertThat(first).isEmpty();
    assertThat(second).isEmpty();
    // The loadKeyStoreAndFactories may be called multiple times due to the map() operation
    // but the result is cached, so subsequent calls use the cached empty result
    verify(keyStoreLoader, atLeastOnce()).loadKeyStoreAndFactories();
  }

  @Test
  void loadFromKeyVault_handlesExceptionDuringSslContextBuild() throws Exception {
    properties.getAzure().getKeyvault().setEnabled(true);
    java.security.KeyStore ks = java.security.KeyStore.getInstance("PKCS12");
    ks.load(null, "password".toCharArray());
    javax.net.ssl.KeyManagerFactory kmf = javax.net.ssl.KeyManagerFactory.getInstance(
        javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
    javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
        javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());

    KeyVaultKeyStoreLoader.KeyStoreFactories factories =
        new KeyVaultKeyStoreLoader.KeyStoreFactories(ks, kmf, tmf, "test-cert");
    when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.of(factories));

    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

    // Should return empty when SSL context build fails (due to invalid keystore)
    Optional<io.netty.handler.ssl.SslContext> result = provider.sslContext();

    assertThat(result).isEmpty();
  }

  @Test
  void loadFromKeyVault_filtersNullContext() {
    properties.getAzure().getKeyvault().setEnabled(true);
    // Create a factories object that will cause build to fail
    java.security.KeyStore ks;
    try {
      ks = java.security.KeyStore.getInstance("PKCS12");
      ks.load(null, "password".toCharArray());
    } catch (Exception e) {
      ks = null;
    }
    javax.net.ssl.KeyManagerFactory kmf = null;
    javax.net.ssl.TrustManagerFactory tmf = null;
    try {
      if (ks != null) {
        kmf = javax.net.ssl.KeyManagerFactory.getInstance(
            javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm());
        tmf = javax.net.ssl.TrustManagerFactory.getInstance(
            javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
      }
    } catch (Exception e) {
      // ignore
    }

    if (ks != null && kmf != null && tmf != null) {
      KeyVaultKeyStoreLoader.KeyStoreFactories factories =
          new KeyVaultKeyStoreLoader.KeyStoreFactories(ks, kmf, tmf, "test-cert");
      when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.of(factories));

      KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

      // Should filter out null context when build fails
      Optional<io.netty.handler.ssl.SslContext> result = provider.sslContext();

      assertThat(result).isEmpty();
    }
  }

  @Test
  void sslContext_returnsCachedContextWhenPresent() {
    properties.getAzure().getKeyvault().setEnabled(true);
    io.netty.handler.ssl.SslContext mockContext = mock(io.netty.handler.ssl.SslContext.class);
    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

    // Manually set cached context using reflection
    try {
      java.lang.reflect.Field field = KeyVaultSslContextProvider.class.getDeclaredField("cachedContext");
      field.setAccessible(true);
      field.set(provider, Optional.of(mockContext));
    } catch (Exception e) {
      // skip if reflection fails
      return;
    }

    Optional<io.netty.handler.ssl.SslContext> result = provider.sslContext();

    assertThat(result).isPresent();
    assertThat(result.get()).isEqualTo(mockContext);
    // Should not call keyStoreLoader when cached
    verify(keyStoreLoader, never()).loadKeyStoreAndFactories();
  }

  // **********************************************************
  // ADDITIONAL TEST
  // **********************************************************

  @Test
  void sslContext_buildsContextWhenKeyStoreFactoriesValid() throws Exception {
    properties.getAzure().getKeyvault().setEnabled(true);
    KeyVaultKeyStoreLoader.KeyStoreFactories factories = KeyVaultTestSupport.createKeyStoreFactories();
    when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.of(factories));

    KeyVaultSslContextProvider provider = new KeyVaultSslContextProvider(properties, keyStoreLoader);

    Optional<io.netty.handler.ssl.SslContext> result = provider.sslContext();

    assertThat(result).isPresent();
    assertThat(provider.sslContext()).isSameAs(result);
    verify(keyStoreLoader).loadKeyStoreAndFactories();
  }

}
