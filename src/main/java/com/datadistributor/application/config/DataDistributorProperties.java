package com.datadistributor.application.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
@ConfigurationProperties(prefix = "data-distributor")
@Validated
public class DataDistributorProperties {

  private ExternalApi externalApi = new ExternalApi();
  private Processing processing = new Processing();
  private Audit audit = new Audit();
  private Http http = new Http();
  private Azure azure = new Azure();
  private Storage storage = new Storage();
  private Scheduler scheduler = new Scheduler();

  @Data
  public static class ExternalApi {
    @NotBlank
    private String baseUrl;
    private String writeSignalPath = "/create-signal/write-signal";
    private String publisher = "UABS";
    private String publisherId = "0bfe5670-457d-4872-a1f1-efe4db39f099";
    @Min(1)
    private long requestTimeoutSeconds = 15;
    private boolean syncEnabled = false;
    private Retry retry = new Retry();

    @Data
    public static class Retry {
      @Min(0)
      private int attempts = 3;
      @Min(0)
      private long backoffSeconds = 5;
      @Min(0)
      private long maxBackoffSeconds = 15;
    }
  }

  @Data
  public static class Processing {
    @Min(1)
    private int batchSize = 300;
    @Min(1)
    private int pageSize = 1000;
    @Min(1)
    private int rateLimit = 20;
    @Min(0)
    private long minUnauthorizedDebitBalance = 250;
    @Min(0)
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

  @Data
  public static class Storage {
    private boolean enabled = false;
    private String connectionString;
    private String container = "reports";
    private String folder = "ceh";
    private String dialFolder = "dial";
    private String dialFilePrefix = "dial-signal-data";
    private boolean dialSchedulerEnabled = true;
  }

  @Data
  public static class Scheduler {
    private boolean enable2am = true;
    private boolean enableMon10 = true;
    private boolean enableMon12 = true;
    private String signal2amCron = "0 0 2 * * *";
    private String signalMon10Cron = "0 0 10 * * MON";
    private String signalMon12Cron = "0 0 12 * * MON";
  }
}
