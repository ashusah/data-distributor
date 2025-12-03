package com.datadistributor.application;

public final class ErrorCodes {
  private ErrorCodes() {}

  public static final String FILE_UPLOAD_FAILED = "LOG_API_001";
  public static final String AUDIT_PERSIST_FAILED = "LOG_AUDIT_001";
  public static final String RECORD_POST_FAILED = "LOG_API_002";
  public static final String CIRCUIT_OPEN = "LOG_API_003";
}
