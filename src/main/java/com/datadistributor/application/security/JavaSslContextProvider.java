package com.datadistributor.application.security;

import java.util.Optional;
import javax.net.ssl.SSLContext;

/**
 * Supplies an optional Java SSL context for Feign HTTP clients.
 * This is separate from Netty's SslContext used by WebClient.
 */
public interface JavaSslContextProvider {

  Optional<SSLContext> sslContext();
}

