package com.datadistributor.application.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.datadistributor.application.config.DataDistributorProperties;
import java.security.KeyStore;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class KeyVaultJavaSslContextProviderTest {

  @Mock
  private KeyVaultKeyStoreLoader keyStoreLoader;

  private DataDistributorProperties properties;

  @BeforeEach
  void setup() {
    MockitoAnnotations.openMocks(this);
    properties = new DataDistributorProperties();
  }

  // *****************************
  // FRESH TEST CASE
  // *****************************

  @Test
  void returnsEmptyWhenKeyVaultDisabled() {
    properties.getAzure().getKeyvault().setEnabled(false);
    KeyVaultJavaSslContextProvider provider = new KeyVaultJavaSslContextProvider(properties, keyStoreLoader);

    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void returnsEmptyWhenKeyStoreLoaderReturnsEmpty() {
    properties.getAzure().getKeyvault().setEnabled(true);
    when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.empty());
    KeyVaultJavaSslContextProvider provider = new KeyVaultJavaSslContextProvider(properties, keyStoreLoader);

    assertThat(provider.sslContext()).isEmpty();
  }

  @Test
  void cachesResultOnSecondCall() {
    properties.getAzure().getKeyvault().setEnabled(false);
    KeyVaultJavaSslContextProvider provider = new KeyVaultJavaSslContextProvider(properties, keyStoreLoader);

    Optional<SSLContext> first = provider.sslContext();
    Optional<SSLContext> second = provider.sslContext();

    assertThat(first).isEqualTo(second);
  }

  @Test
  void returnsEmptyWhenSslContextInitFails() throws Exception {
    properties.getAzure().getKeyvault().setEnabled(true);
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(null, "password".toCharArray());
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    
    KeyVaultKeyStoreLoader.KeyStoreFactories factories = 
        new KeyVaultKeyStoreLoader.KeyStoreFactories(ks, kmf, tmf, "test-cert");
    when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.of(factories));
    
    KeyVaultJavaSslContextProvider provider = new KeyVaultJavaSslContextProvider(properties, keyStoreLoader);

    // Should return empty when SSL context initialization fails
    assertThat(provider.sslContext()).isEmpty();
  }

  // **********************************************************
  // ADDITIONAL TEST
  // *********************************************************

  @Test
  void returnsSslContextWhenKeyStoreFactoriesValid() throws Exception {
    properties.getAzure().getKeyvault().setEnabled(true);
    KeyStore ks = KeyStore.getInstance("PKCS12");
    char[] password = "password".toCharArray();
    ks.load(null, password);
    KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
    kmf.init(ks, password);
    TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    tmf.init(ks);

    KeyVaultKeyStoreLoader.KeyStoreFactories factories =
        new KeyVaultKeyStoreLoader.KeyStoreFactories(ks, kmf, tmf, "test-cert");
    when(keyStoreLoader.loadKeyStoreAndFactories()).thenReturn(Optional.of(factories));

    KeyVaultJavaSslContextProvider provider = new KeyVaultJavaSslContextProvider(properties, keyStoreLoader);

    assertThat(provider.sslContext()).isPresent();
  }
}
