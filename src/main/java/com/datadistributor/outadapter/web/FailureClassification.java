package com.datadistributor.outadapter.web;

/**
 * Categorization of a failed delivery attempt, used to log and decide retry behavior.
 */
public record FailureClassification(String status, String reason, String responseCode) {
}
