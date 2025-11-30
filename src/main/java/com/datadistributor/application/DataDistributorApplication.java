package com.datadistributor.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.datadistributor")
public class DataDistributorApplication {

  public static void main(String[] args) {
    SpringApplication.run(DataDistributorApplication.class, args);
  }
}
