package com.datadistributor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import com.datadistributor.outadapter.web.SignalEventPayload;
import com.datadistributor.outadapter.web.SignalEventPayloadFactory;
import com.datadistributor.support.StubExternalApiConfig;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
    classes = {com.datadistributor.application.DataDistributorApplication.class, StubExternalApiConfig.class},
    webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "data-distributor.external-api.publisher=CONFIG-PUB",
    "data-distributor.external-api.publisher-id=CONFIG-PUB-ID",
    "data-distributor.external-api.request-timeout-seconds=7",
    "data-distributor.external-api.retry.attempts=4",
    "data-distributor.external-api.retry.backoff-seconds=2",
    "data-distributor.external-api.retry.max-backoff-seconds=8",
    "data-distributor.http.connect-timeout-ms=1234",
    "data-distributor.http.response-timeout-seconds=6",
    "data-distributor.http.read-timeout-seconds=6",
    "data-distributor.http.write-timeout-seconds=6"
})
class ConfigBindingIntegrationTest {

  @Autowired
  private DataDistributorProperties properties;

  @Autowired
  private SignalEventPayloadFactory payloadFactory;

  @Test
  void propertiesBindAndFlowIntoPayloadFactory() {
    // properties bound
    assertThat(properties.getExternalApi().getPublisher()).isEqualTo("CONFIG-PUB");
    assertThat(properties.getExternalApi().getPublisherId()).isEqualTo("CONFIG-PUB-ID");
    assertThat(properties.getExternalApi().getRequestTimeoutSeconds()).isEqualTo(7);
    assertThat(properties.getExternalApi().getRetry().getAttempts()).isEqualTo(4);
    assertThat(properties.getExternalApi().getRetry().getBackoffSeconds()).isEqualTo(2);
    assertThat(properties.getExternalApi().getRetry().getMaxBackoffSeconds()).isEqualTo(8);
    assertThat(properties.getHttp().getConnectTimeoutMs()).isEqualTo(1234);
    assertThat(properties.getHttp().getResponseTimeoutSeconds()).isEqualTo(6);
    assertThat(properties.getHttp().getReadTimeoutSeconds()).isEqualTo(6);
    assertThat(properties.getHttp().getWriteTimeoutSeconds()).isEqualTo(6);

    // payload picks up publisher settings
    SignalEvent event = new SignalEvent();
    event.setAgreementId(1L);
    event.setSignalId(2L);
    event.setEventStatus("OVERLIMIT_SIGNAL");
    event.setEventType("CONTRACT_UPDATE");
    event.setEventRecordDateTime(LocalDateTime.now());

    SignalEventPayload payload = payloadFactory.buildPayload(event);
    assertThat(payload.getPublisher()).isEqualTo("CONFIG-PUB");
    assertThat(payload.getPublisherId()).isEqualTo("CONFIG-PUB-ID");
  }
}
