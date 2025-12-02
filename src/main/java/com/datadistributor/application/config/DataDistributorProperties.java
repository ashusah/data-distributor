package com.datadistributor.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "data-distributor")
public class DataDistributorProperties {

  private ExternalApi externalApi = new ExternalApi();
  private Processing processing = new Processing();
  private Audit audit = new Audit();
  private Http http = new Http();
  private Azure azure = new Azure();

  @Data
  public static class ExternalApi {
    private String baseUrl;
    private String writeSignalPath = "/create-signal/write-signal";
    private String publisher = "UABS";
    private String publisherId = "0bfe5670-457d-4872-a1f1-efe4db39f099";
    private long requestTimeoutSeconds = 15;
    private boolean syncEnabled = false;
    private Retry retry = new Retry();

    @Data
    public static class Retry {
      private int attempts = 3;
      private long backoffSeconds = 5;
      private long maxBackoffSeconds = 15;
    }
  }

  @Data
  public static class Processing {
    private int batchSize = 300;
    private int rateLimit = 20;
    private long minUnauthorizedDebitBalance = 250;
    private int bookDateLookbackDays = 5;
  }

  @Data
  public static class Audit {
    private long consumerId = 1;
  }

  @Data
  public static class Http {
    private int connectTimeoutMs = 10_000;
    private long responseTimeoutSeconds = 10;
    private int readTimeoutSeconds = 10;
    private int writeTimeoutSeconds = 10;
  }

  @Data
  public static class Azure {
    private KeyVault keyvault = new KeyVault();

    @Data
    public static class KeyVault {
      private boolean enabled = false;
      private String vaultUrl;
      private String certificateName;
      private String certificatePassword;
      private String clientId;
    }
  }
}
