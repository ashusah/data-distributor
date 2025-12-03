package com.datadistributor.outadapter.web;

import java.util.Map;

/**
 * Normalized API response wrapper for outbound CEH calls.
 */
public record ApiResponse(Map<String, Object> body, int statusCode) {
}
