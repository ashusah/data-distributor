package com.datadistributor.outadapter.report;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.outport.DeliveryReportPublisher;
import com.datadistributor.domain.report.DeliveryReport;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

@Slf4j
public class AzureBlobReportPublisher implements DeliveryReportPublisher {

  private final AzureBlobStorageClient storageClient;
  private final DataDistributorProperties.Storage storage;

  public AzureBlobReportPublisher(DataDistributorProperties properties) {
    this.storage = properties.getStorage();
    this.storageClient = new AzureBlobStorageClient(properties);
  }

  @Override
  public void publish(DeliveryReport report) {
    String blobName = buildBlobName(report);
    storageClient.upload(storage.getFolder(), blobName, report.getContent());
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
