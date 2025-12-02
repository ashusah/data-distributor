package com.datadistributor.outadapter.report;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.datadistributor.application.config.DataDistributorProperties;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class AzureBlobStorageClient {

  private final DataDistributorProperties.Storage storage;
  private final BlobServiceClient serviceClient;

  public AzureBlobStorageClient(DataDistributorProperties properties) {
    this.storage = properties.getStorage();
    this.serviceClient = buildClient(storage);
  }

  public void upload(String folder, String fileName, String content) {
    if (!isEnabled()) {
      log.debug("Storage disabled; skipping upload for {}", fileName);
      return;
    }
    try {
      BlobContainerClient containerClient = serviceClient.getBlobContainerClient(storage.getContainer());
      if (!containerClient.exists()) {
        containerClient.create();
      }
      String blobPath = buildPath(folder, fileName);
      containerClient.getBlobClient(blobPath)
          .upload(BinaryData.fromString(content), true);
      log.info("Uploaded blob {} to container {}", blobPath, storage.getContainer());
    } catch (Exception ex) {
      log.error("Failed to upload blob {}: {}", fileName, ex.getMessage(), ex);
    }
  }

  private boolean isEnabled() {
    return storage.isEnabled() && serviceClient != null;
  }

  private BlobServiceClient buildClient(DataDistributorProperties.Storage storage) {
    if (!storage.isEnabled()) return null;
    if (!StringUtils.hasText(storage.getConnectionString())) return null;
    return new BlobServiceClientBuilder()
        .connectionString(storage.getConnectionString())
        .buildClient();
  }

  private String buildPath(String folder, String fileName) {
    String sanitizedFolder = Optional.ofNullable(folder).orElse("").strip();
    if (sanitizedFolder.isEmpty()) {
      return fileName;
    }
    return sanitizedFolder.endsWith("/") ? sanitizedFolder + fileName : sanitizedFolder + "/" + fileName;
  }
}
