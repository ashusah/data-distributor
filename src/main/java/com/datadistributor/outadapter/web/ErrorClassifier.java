package com.datadistributor.outadapter.web;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.netty.http.client.PrematureCloseException;

@Component
public class ErrorClassifier {

  public FailureClassification classify(Throwable ex) {
    Throwable root = unwrap(ex);
    String status;
    String reason;

    if (root instanceof WebClientResponseException wcre) {
      int code = wcre.getRawStatusCode();
      if (code == 429 || (code >= 500 && code < 600)) {
        status = "FAIL_TRANSIENT";
        reason = shortReason(root, "HTTP_" + code);
      } else {
        status = "FAIL_PERMANENT";
        reason = shortReason(root, "HTTP_" + code);
      }
    } else if (root instanceof java.util.concurrent.TimeoutException) {
      status = "TIMEOUT";
      reason = shortReason(root, "TIMEOUT");
    } else if (root instanceof io.github.resilience4j.circuitbreaker.CallNotPermittedException) {
      status = "BLOCKED_BY_CIRCUIT";
      reason = "BLOCKED_BY_CIRCUIT";
    } else if (root instanceof WebClientRequestException
        || root instanceof PrematureCloseException
        || root instanceof IOException) {
      status = "FAIL_TRANSIENT";
      reason = shortReason(root, "IO_ERROR");
    } else if (root instanceof InterruptedException) {
      status = "INTERRUPTED";
      reason = "INTERRUPTED";
    } else {
      status = "FAIL_UNKNOWN";
      reason = shortReason(root, "UNKNOWN");
    }

    String responseCode;
    if (root instanceof WebClientResponseException wcrex) {
      responseCode = String.valueOf(wcrex.getRawStatusCode());
    } else {
      responseCode = "N/A";
    }

    return new FailureClassification(status, reason, responseCode);
  }

  private String shortReason(Throwable ex, String defaultReason) {
    String cls = ex == null ? null : ex.getClass().getSimpleName();
    if (cls == null || cls.isBlank()) {
      return defaultReason;
    }
    return cls.length() > 32 ? cls.substring(0, 32) : cls;
  }

  private Throwable unwrap(Throwable ex) {
    if (ex == null) return null;
    Throwable current = ex;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current;
  }
}
