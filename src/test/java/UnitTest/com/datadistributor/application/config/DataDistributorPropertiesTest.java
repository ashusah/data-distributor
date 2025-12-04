package com.datadistributor.application.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Basic sanity checks for defaults in {@link DataDistributorProperties}.
 */
class DataDistributorPropertiesTest {

  @Test
  void defaultsAreInitialized() {
    DataDistributorProperties props = new DataDistributorProperties();
    assertThat(props.getExternalApi().getWriteSignalPath()).isEqualTo("/create-signal/write-signal");
    assertThat(props.getProcessing().getBatchSize()).isPositive();
    assertThat(props.getScheduler().isEnableRetry()).isTrue();
    assertThat(props.getStorage().getContainer()).isEqualTo("reports");
  }
}
