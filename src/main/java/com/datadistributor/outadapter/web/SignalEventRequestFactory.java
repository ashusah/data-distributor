package com.datadistributor.outadapter.web;

import com.datadistributor.application.config.DataDistributorProperties;
import com.datadistributor.domain.SignalEvent;
import org.springframework.stereotype.Component;

@Component
public class SignalEventRequestFactory {

  private final DataDistributorProperties properties;
  private final SignalEventPayloadFactory payloadFactory;

  public SignalEventRequestFactory(DataDistributorProperties properties,
                                   SignalEventPayloadFactory payloadFactory) {
    this.properties = properties;
    this.payloadFactory = payloadFactory;
  }

  public SignalEventRequest build(SignalEvent event) {
    String uri = properties.getExternalApi().getBaseUrl() + properties.getExternalApi().getWriteSignalPath();
    return new SignalEventRequest(uri, payloadFactory.buildPayload(event));
  }
}
