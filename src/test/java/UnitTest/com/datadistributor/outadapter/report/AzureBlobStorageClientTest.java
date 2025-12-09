package com.datadistributor.outadapter.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.outadapter.report.AzureBlobStorageClient.BlobClientAdapter;
import com.datadistributor.outadapter.report.AzureBlobStorageClient.BlobContainerClientAdapter;
import com.datadistributor.outadapter.report.AzureBlobStorageClient.BlobServiceClientAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Covers upload path and path-building logic for AzureBlobStorageClient without mocking finals.
 */
class AzureBlobStorageClientTest {

  private RecordingContainer container;
  private AzureBlobStorageClient client;

  @BeforeEach
  void setup() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getStorage().setEnabled(true);
    properties.getStorage().setContainer("my-container");
    container = new RecordingContainer(false);
    client = new AzureBlobStorageClient(properties, new RecordingService(container));
  }

  @Test
  void uploadCreatesContainerWhenMissingAndBuildsPath() {
    assertThatCode(() -> client.upload("folder", "file.txt", "content")).doesNotThrowAnyException();

    assertThat(container.created).isTrue();
    assertThat(container.lastPath).isEqualTo("folder/file.txt");
    assertThat(container.lastContent).isEqualTo("content");
  }

  @Test
  void uploadHandlesEmptyFolder() {
    container.exists = true; // already present

    client.upload("", "file.txt", "content");

    assertThat(container.created).isFalse();
    assertThat(container.lastPath).isEqualTo("file.txt");
  }

  private static class RecordingService implements BlobServiceClientAdapter {
    private final RecordingContainer container;

    RecordingService(RecordingContainer container) {
      this.container = container;
    }

    @Override
    public BlobContainerClientAdapter getBlobContainerClient(String containerName) {
      return container;
    }
  }

  private static class RecordingContainer implements BlobContainerClientAdapter {
    private boolean exists;
    private boolean created;
    private String lastPath;
    private String lastContent;

    RecordingContainer(boolean exists) {
      this.exists = exists;
    }

    @Override
    public boolean exists() {
      return exists;
    }

    @Override
    public void create() {
      this.created = true;
      this.exists = true;
    }

    @Override
    public BlobClientAdapter getBlobClient(String path) {
      this.lastPath = path;
      return content -> this.lastContent = content;
    }
  }

  // ************************************************************************************************
  // NEW COMPREHENSIVE TESTS FOR 100% COVERAGE
  // ************************************************************************************************

  @Test
  void upload_skipsWhenStorageDisabled() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getStorage().setEnabled(false);
    AzureBlobStorageClient disabledClient = new AzureBlobStorageClient(properties);

    assertThatCode(() -> disabledClient.upload("folder", "file.txt", "content"))
        .doesNotThrowAnyException();
  }

  @Test
  void upload_handlesNullServiceClient() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getStorage().setEnabled(true);
    AzureBlobStorageClient clientWithNullService = new AzureBlobStorageClient(properties, null);

    assertThatCode(() -> clientWithNullService.upload("folder", "file.txt", "content"))
        .doesNotThrowAnyException();
  }

  @Test
  void upload_handlesExceptionDuringUpload() {
    DataDistributorProperties properties = new DataDistributorProperties();
    properties.getStorage().setEnabled(true);
    properties.getStorage().setContainer("my-container");
    BlobServiceClientAdapter failingAdapter = new BlobServiceClientAdapter() {
      @Override
      public BlobContainerClientAdapter getBlobContainerClient(String containerName) {
        return new BlobContainerClientAdapter() {
          @Override
          public boolean exists() {
            return true;
          }

          @Override
          public void create() {
            throw new RuntimeException("Create failed");
          }

          @Override
          public BlobClientAdapter getBlobClient(String path) {
            throw new RuntimeException("Get blob failed");
          }
        };
      }
    };
    AzureBlobStorageClient clientWithFailingAdapter = new AzureBlobStorageClient(properties, failingAdapter);

    assertThatCode(() -> clientWithFailingAdapter.upload("folder", "file.txt", "content"))
        .doesNotThrowAnyException();
  }

  @Test
  void upload_handlesFolderWithTrailingSlash() {
    container.exists = true;
    client.upload("folder/", "file.txt", "content");

    assertThat(container.lastPath).isEqualTo("folder/file.txt");
  }

  @Test
  void upload_handlesFolderWithoutTrailingSlash() {
    container.exists = true;
    client.upload("folder", "file.txt", "content");

    assertThat(container.lastPath).isEqualTo("folder/file.txt");
  }

  @Test
  void upload_handlesNullFolder() {
    container.exists = true;
    client.upload(null, "file.txt", "content");

    assertThat(container.lastPath).isEqualTo("file.txt");
  }

  @Test
  void upload_handlesEmptyFolder() {
    container.exists = true;
    client.upload("   ", "file.txt", "content");

    assertThat(container.lastPath).isEqualTo("file.txt");
  }

  @Test
  void buildPath_handlesWhitespaceOnlyFolder() {
    container.exists = true;
    client.upload("\t\n", "file.txt", "content");

    assertThat(container.lastPath).isEqualTo("file.txt");
  }
}
