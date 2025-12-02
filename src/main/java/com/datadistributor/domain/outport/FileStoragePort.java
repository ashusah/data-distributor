package com.datadistributor.domain.outport;

public interface FileStoragePort {
  void upload(String folder, String fileName, String content);
}
