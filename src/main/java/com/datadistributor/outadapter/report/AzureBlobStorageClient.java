package com.datadistributor.outadapter.report;

import com.azure.identity.ManagedIdentityCredential;
import com.azure.identity.ManagedIdentityCredentialBuilder;
import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.outport.FileStoragePort;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
/**
 * Azure Blob Storage implementation of {@link FileStoragePort}. Handles container creation,
 * path resolution, and skips uploads when storage is disabled via configuration.
 */
public class AzureBlobStorageClient implements FileStoragePort {

  private final DataDistributorProperties.Storage storage;
  private final BlobServiceClientAdapter serviceClient;

  public AzureBlobStorageClient(DataDistributorProperties properties) {
    this.storage = properties.getStorage();
    this.serviceClient = buildClient(storage);
  }

  /**
   * Test-support constructor to inject a custom client adapter (e.g., a simple stub).
   */
  AzureBlobStorageClient(DataDistributorProperties properties, BlobServiceClientAdapter adapter) {
    this.storage = properties.getStorage();
    this.serviceClient = adapter;
  }

  @Override
  public void upload(String folder, String fileName, String content) {
    if (!isEnabled()) {
      log.debug("Storage disabled; skipping upload for {}", fileName);
      return;
    }
    try {
      BlobContainerClientAdapter containerClient = serviceClient.getBlobContainerClient(storage.getContainer());
      if (!containerClient.exists()) {
        containerClient.create();
      }
      String blobPath = buildPath(folder, fileName);
      containerClient.getBlobClient(blobPath).upload(content);
      log.info("Uploaded blob {} to container {}", blobPath, storage.getContainer());
    } catch (Exception ex) {
      log.error("{}: Failed to upload blob {}: {}", com.datadistributor.application.ErrorCodes.FILE_UPLOAD_FAILED, fileName, ex.getMessage(), ex);
    }
  }

  private boolean isEnabled() {
    return storage.isEnabled() && serviceClient != null;
  }

  private BlobServiceClientAdapter buildClient(DataDistributorProperties.Storage storage) {
    if (!storage.isEnabled()) {
      return null;
    }
    if (hasManagedIdentityConfig(storage)) {
      return buildManagedIdentityClient(storage);
    }
    if (StringUtils.hasText(storage.getConnectionString())) {
      return new DefaultBlobServiceClientAdapter(storage.getConnectionString());
    }
    return null;
  }

  private boolean hasManagedIdentityConfig(DataDistributorProperties.Storage storage) {
    return StringUtils.hasText(storage.getStorageUrl())
        && StringUtils.hasText(storage.getManagedIdentityClientId());
  }

  private BlobServiceClientAdapter buildManagedIdentityClient(DataDistributorProperties.Storage storage) {
    ManagedIdentityCredential credential = new ManagedIdentityCredentialBuilder()
        .clientId(storage.getManagedIdentityClientId())
        .build();
    return new DefaultBlobServiceClientAdapter(storage.getStorageUrl(), credential);
  }

  private String buildPath(String folder, String fileName) {
    String sanitizedFolder = Optional.ofNullable(folder).orElse("").strip();
    if (sanitizedFolder.isEmpty()) {
      return fileName;
    }
    return sanitizedFolder.endsWith("/") ? sanitizedFolder + fileName : sanitizedFolder + "/" + fileName;
  }

  /**
   * Narrow interface to keep Azure SDK types out of tests that do not need them.
   */
  interface BlobServiceClientAdapter {
    BlobContainerClientAdapter getBlobContainerClient(String containerName);
  }

  interface BlobContainerClientAdapter {
    boolean exists();

    void create();

    BlobClientAdapter getBlobClient(String path);
  }

  interface BlobClientAdapter {
    void upload(String content);
  }

  static class DefaultBlobServiceClientAdapter implements BlobServiceClientAdapter {
    private final com.azure.storage.blob.BlobServiceClient delegate;

    DefaultBlobServiceClientAdapter(String connectionString) {
      this.delegate = new com.azure.storage.blob.BlobServiceClientBuilder()
          .connectionString(connectionString)
          .buildClient();
    }

    DefaultBlobServiceClientAdapter(String endpoint, ManagedIdentityCredential credential) {
      this.delegate = new com.azure.storage.blob.BlobServiceClientBuilder()
          .endpoint(endpoint)
          .credential(credential)
          .buildClient();
    }

    @Override
    public BlobContainerClientAdapter getBlobContainerClient(String containerName) {
      return new AzureBlobContainerClientAdapter(delegate.getBlobContainerClient(containerName));
    }
  }

  static class AzureBlobContainerClientAdapter implements BlobContainerClientAdapter {
    private final com.azure.storage.blob.BlobContainerClient delegate;

    AzureBlobContainerClientAdapter(com.azure.storage.blob.BlobContainerClient delegate) {
      this.delegate = delegate;
    }

    @Override
    public boolean exists() {
      return delegate.exists();
    }

    @Override
    public void create() {
      delegate.create();
    }

    @Override
    public BlobClientAdapter getBlobClient(String path) {
      return new AzureBlobClientAdapter(delegate.getBlobClient(path));
    }
  }

  static class AzureBlobClientAdapter implements BlobClientAdapter {
    private final com.azure.storage.blob.BlobClient delegate;

    AzureBlobClientAdapter(com.azure.storage.blob.BlobClient delegate) {
      this.delegate = delegate;
    }

    @Override
    public void upload(String content) {
      delegate.upload(com.azure.core.util.BinaryData.fromString(content), true);
    }
  }
}
