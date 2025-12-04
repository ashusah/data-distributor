package com.datadistributor.outadapter.web;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Minimal response body returned by CEH after posting a signal event.
 */
public record SignalEventResponse(
    @JsonProperty("ceh_event_id") Long cehEventId
) {
}
