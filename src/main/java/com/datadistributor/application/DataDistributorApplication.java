package com.datadistributor.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Data Distributor application, enabling Spring Boot auto configuration and
 * scheduled jobs.
 */
@SpringBootApplication(scanBasePackages = "com.datadistributor")
@EnableScheduling
public class DataDistributorApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataDistributorApplication.class, args);
  }
}
