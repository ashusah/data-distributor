package com.datadistributor.application.security;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import java.io.ByteArrayInputStream;
import java.security.KeyStore;
import java.util.Base64;
import java.util.Optional;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KeyVaultSslContextProvider implements SslContextProvider {

  private final boolean enabled;
  private final String vaultUrl;
  private final String certificateName;
  private final String certificatePassword;
  private final String managedIdentityClientId;

  private volatile Optional<SslContext> cachedContext = Optional.empty();

  public KeyVaultSslContextProvider(
      @Value("${azure.keyvault.enabled:false}") boolean enabled,
      @Value("${azure.keyvault.vault-url:}") String vaultUrl,
      @Value("${azure.keyvault.certificate-name:}") String certificateName,
      @Value("${azure.keyvault.certificate-password:}") String certificatePassword,
      @Value("${azure.keyvault.client-id:}") String managedIdentityClientId) {
    this.enabled = enabled;
    this.vaultUrl = vaultUrl;
    this.certificateName = certificateName;
    this.certificatePassword = certificatePassword;
    this.managedIdentityClientId = managedIdentityClientId;
  }

  @Override
  public Optional<SslContext> sslContext() {
    if (!enabled) {
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
    try {
      if (vaultUrl == null || vaultUrl.isBlank() || certificateName == null || certificateName.isBlank()) {
        log.warn("KeyVault SSL context not loaded: vaultUrl or certificateName missing");
        return Optional.empty();
      }

      var credentialBuilder = new DefaultAzureCredentialBuilder();
      if (managedIdentityClientId != null && !managedIdentityClientId.isBlank()) {
        credentialBuilder = credentialBuilder.managedIdentityClientId(managedIdentityClientId);
      }

      SecretClient secretClient = new SecretClientBuilder()
          .vaultUrl(vaultUrl)
          .credential(credentialBuilder.build())
          .buildClient();

      KeyVaultSecret secret = secretClient.getSecret(certificateName);
      byte[] pfxBytes = Base64.getDecoder().decode(secret.getValue());

      char[] pwd = certificatePassword == null ? new char[0] : certificatePassword.toCharArray();
      KeyStore ks = KeyStore.getInstance("PKCS12");
      ks.load(new ByteArrayInputStream(pfxBytes), pwd);

      KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      kmf.init(ks, pwd);

      TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
      tmf.init(ks);

      SslContext context = SslContextBuilder.forClient()
          .keyManager(kmf)
          .trustManager(tmf)
          .build();
      log.info("SSL context loaded from Key Vault certificate {}", certificateName);
      return Optional.of(context);
    } catch (Exception ex) {
      log.error("Failed to load SSL context from Key Vault: {}", ex.getMessage(), ex);
      return Optional.empty();
    }
  }
}
