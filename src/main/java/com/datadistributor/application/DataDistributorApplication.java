package com.datadistributor.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.datadistributor")
@EnableScheduling
public class DataDistributorApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataDistributorApplication.class, args);
  }
}
