package com.datadistributor.domain.outport;

public interface FileStoragePort {
  /**
   * Uploads content to the specified folder/file location.
   */
  void upload(String folder, String fileName, String content);
}
