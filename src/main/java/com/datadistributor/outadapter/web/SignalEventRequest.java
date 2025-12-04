package com.datadistributor.outadapter.web;

/**
 * Bundles the target URI with the outbound payload for the web client implementation.
 */
public record SignalEventRequest(String uri, SignalEventPayload payload) {
}
