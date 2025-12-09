package com.datadistributor.application.security;

import com.datadistributor.application.config.DataDistributorProperties;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Loads a Netty {@link SslContext} from Azure Key Vault certificates, caching the result for reuse.
 */
@Component
@Slf4j
public class KeyVaultSslContextProvider implements SslContextProvider {

  private final DataDistributorProperties properties;
  private final KeyVaultKeyStoreLoader keyStoreLoader;

  private volatile Optional<SslContext> cachedContext = Optional.empty();

  public KeyVaultSslContextProvider(
      DataDistributorProperties properties,
      KeyVaultKeyStoreLoader keyStoreLoader) {
    this.properties = properties;
    this.keyStoreLoader = keyStoreLoader;
  }

  @Override
  public Optional<SslContext> sslContext() {
    var kv = properties.getAzure().getKeyvault();
    if (!kv.isEnabled()) {
      return Optional.empty();
    }
    if (cachedContext.isPresent()) {
      return cachedContext;
    }
    synchronized (this) {
      if (cachedContext.isPresent()) {
        return cachedContext;
      }
      cachedContext = loadFromKeyVault();
      return cachedContext;
    }
  }

  private Optional<SslContext> loadFromKeyVault() {
    return keyStoreLoader.loadKeyStoreAndFactories()
        .map(factories -> {
          try {
            SslContext context = SslContextBuilder.forClient()
                .keyManager(factories.getKeyManagerFactory())
                .trustManager(factories.getTrustManagerFactory())
                .build();
            log.info("SSL context loaded from Key Vault certificate {}", factories.getCertificateName());
            return context;
          } catch (Exception ex) {
            log.error("Failed to build SSL context: {}", ex.getMessage(), ex);
            return null;
          }
        })
        .filter(context -> context != null);
  }
}
