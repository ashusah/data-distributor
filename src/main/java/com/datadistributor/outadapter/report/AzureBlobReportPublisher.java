package com.datadistributor.outadapter.report;

import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.report.DeliveryReport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class AzureBlobReportPublisher implements DeliveryReportPublisher {

  private final BlobServiceClient serviceClient;
  private final DataDistributorProperties.Storage storage;

  public AzureBlobReportPublisher(DataDistributorProperties properties) {
    this.storage = properties.getStorage();
    if (storage.isEnabled() && StringUtils.hasText(storage.getConnectionString())) {
      this.serviceClient = new BlobServiceClientBuilder()
          .connectionString(storage.getConnectionString())
          .buildClient();
    } else {
      this.serviceClient = null;
    }
  }

  @Override
  public void publish(DeliveryReport report) {
    if (!storage.isEnabled() || serviceClient == null) {
      log.debug("Storage reporting disabled; skipping upload for {}", report.getDate());
      return;
    }
    try {
      BlobContainerClient containerClient = serviceClient.getBlobContainerClient(storage.getContainer());
      if (!containerClient.exists()) {
        containerClient.create();
      }
      String blobName = buildBlobName(report);
      containerClient.getBlobClient(blobName)
          .upload(BinaryData.fromString(report.getContent()), true);
      log.info("Uploaded delivery report for {} to container {} as {}", report.getDate(), storage.getContainer(), blobName);
    } catch (Exception ex) {
      log.error("Failed to upload delivery report for {}: {}", report.getDate(), ex.getMessage(), ex);
    }
  }

  private String buildBlobName(DeliveryReport report) {
    String folder = StringUtils.hasText(storage.getFolder()) ? storage.getFolder().strip() : "";
    String timestamp = LocalDateTime.now().withNano(0).toString().replace(":", "-");
    String fileName = "ceh-report-" + report.getDate() + "-" + timestamp + ".txt";
    if (folder.isEmpty()) {
      return fileName;
    }
    return folder.endsWith("/") ? folder + fileName : folder + "/" + fileName;
  }
}
