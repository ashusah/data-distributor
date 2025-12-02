package com.datadistributor.outadapter.web;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SignalEventResponse(
    @JsonProperty("ceh_event_id") Long cehEventId
) {
}
