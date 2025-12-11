package com.datadistributor.application.security;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import com.datadistributor.application.config.DataDistributorProperties;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Shared helper to load KeyStore and factories from Azure Key Vault.
 * Used by both Netty and Java SSL context providers to avoid code duplication.
 */
@Component
@Slf4j
public class KeyVaultKeyStoreLoader {

  private final DataDistributorProperties properties;

  public KeyVaultKeyStoreLoader(DataDistributorProperties properties) {
    this.properties = properties;
  }

  /**
   * Loads KeyStore, KeyManagerFactory, and TrustManagerFactory from Azure Key Vault.
   * Returns empty if Key Vault is disabled or configuration is missing.
   */
  public Optional<KeyStoreFactories> loadKeyStoreAndFactories() {
    try {
      var kv = properties.getAzure().getKeyvault();
      if (!kv.isEnabled()) {
        return Optional.empty();
      }

      String vaultUrl = kv.getVaultUrl();
      String certificateName = kv.getCertificateName();
      String certificatePassword = kv.getCertificatePassword();

      if (vaultUrl == null || vaultUrl.isBlank() || certificateName == null || certificateName.isBlank()) {
        log.warn("KeyVault SSL context not loaded: vaultUrl or certificateName missing");
        return Optional.empty();
      }

      DefaultAzureCredentialBuilder credentialBuilder = createCredentialBuilder();
      String managedIdentityClientId = kv.getClientId();
      if (managedIdentityClientId != null && !managedIdentityClientId.isBlank()) {
        credentialBuilder = credentialBuilder.managedIdentityClientId(managedIdentityClientId);
      }

      String secretBase64 = fetchSecretValue(kv, vaultUrl, certificateName, credentialBuilder);
      byte[] pfxBytes = Base64.getDecoder().decode(secretBase64);

      char[] pwd = certificatePassword == null ? new char[0] : certificatePassword.toCharArray();
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(new ByteArrayInputStream(pfxBytes), pwd);

      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(ks, pwd);

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(ks);

      return Optional.of(new KeyStoreFactories(ks, kmf, tmf, certificateName));
    } catch (Exception ex) {
      log.error("Failed to load KeyStore from Key Vault: {}", ex.getMessage(), ex);
      return Optional.empty();
    }
  }

  protected String fetchSecretValue(DataDistributorProperties.Azure.KeyVault kv,
                                    String vaultUrl,
                                    String certificateName,
                                    DefaultAzureCredentialBuilder credentialBuilder) {
    SecretClient secretClient = new SecretClientBuilder()
        .vaultUrl(vaultUrl)
        .credential(credentialBuilder.build())
        .buildClient();

    KeyVaultSecret secret = secretClient.getSecret(certificateName);
    return secret.getValue();
  }

  protected DefaultAzureCredentialBuilder createCredentialBuilder() {
    return new DefaultAzureCredentialBuilder();
  }

  /**
   * Container for KeyStore and factories loaded from Key Vault.
   */
  public static class KeyStoreFactories {
    private final KeyStore keyStore;
    private final KeyManagerFactory keyManagerFactory;
    private final TrustManagerFactory trustManagerFactory;
    private final String certificateName;

    public KeyStoreFactories(KeyStore keyStore, KeyManagerFactory keyManagerFactory,
                            TrustManagerFactory trustManagerFactory, String certificateName) {
      this.keyStore = keyStore;
      this.keyManagerFactory = keyManagerFactory;
      this.trustManagerFactory = trustManagerFactory;
      this.certificateName = certificateName;
    }

    public KeyStore getKeyStore() {
      return keyStore;
    }

    public KeyManagerFactory getKeyManagerFactory() {
      return keyManagerFactory;
    }

    public TrustManagerFactory getTrustManagerFactory() {
      return trustManagerFactory;
    }

    public String getCertificateName() {
      return certificateName;
    }
  }
}
