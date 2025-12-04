package com.datadistributor.outadapter.report;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.outport.FileStoragePort;
import com.datadistributor.domain.report.DeliveryReport;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AzureBlobComponentsTest {

  @Test
  void azureBlobStorageClient_skipsWhenDisabled() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getStorage().setEnabled(false);
    AzureBlobStorageClient client = new AzureBlobStorageClient(properties);

    assertThatCode(() -> client.upload("folder", "file.txt", "content")).doesNotThrowAnyException();
  }

  @Test
  void azureBlobReportPublisher_delegatesToStorageClient() {
    FileStoragePort storage = Mockito.mock(FileStoragePort.class);
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getStorage().setFolder("reports");
    AzureBlobReportPublisher publisher = new AzureBlobReportPublisher(properties, storage);

    DeliveryReport report = DeliveryReport.builder()
        .date(LocalDate.of(2024, 1, 1))
        .totalEvents(1)
        .successEvents(1)
        .failedEvents(0)
        .content("hello")
        .build();

    publisher.publish(report);

    verify(storage).upload(eq("reports"), anyString(), eq("hello"));
  }
}
