package com.datadistributor.application.security;

import com.datadistributor.application.config.DataDistributorProperties;
import java.util.Optional;
import javax.net.ssl.SSLContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Loads a Java {@link SSLContext} from Azure Key Vault certificates for Feign clients,
 * caching the result for reuse. Only loads when Key Vault is enabled.
 */
@Component
@Slf4j
public class KeyVaultJavaSslContextProvider implements JavaSslContextProvider {

  private final DataDistributorProperties properties;
  private final KeyVaultKeyStoreLoader keyStoreLoader;

  private volatile Optional<SSLContext> cachedContext = Optional.empty();

  public KeyVaultJavaSslContextProvider(DataDistributorProperties properties,
                                        KeyVaultKeyStoreLoader keyStoreLoader) {
    this.properties = properties;
    this.keyStoreLoader = keyStoreLoader;
  }

  @Override
  public Optional<SSLContext> sslContext() {
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

  private Optional<SSLContext> loadFromKeyVault() {
    return keyStoreLoader.loadKeyStoreAndFactories()
        .map(factories -> {
          try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(factories.getKeyManagerFactory().getKeyManagers(),
                factories.getTrustManagerFactory().getTrustManagers(), null);
            log.info("Java SSL context loaded from Key Vault certificate {} for Feign clients",
                factories.getCertificateName());
            return context;
          } catch (Exception ex) {
            log.error("Failed to initialize Java SSL context: {}", ex.getMessage(), ex);
            return null;
          }
        })
        .filter(context -> context != null);
  }
}

