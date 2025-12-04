package com.datadistributor.application.security;

import io.netty.handler.ssl.SslContext;
import java.util.Optional;

/**
 * Supplies an optional SSL context for outbound HTTP clients.
 */
public interface SslContextProvider {

  Optional<SslContext> sslContext();
}
