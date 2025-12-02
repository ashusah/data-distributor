package com.datadistributor.application.security;

import io.netty.handler.ssl.SslContext;
import java.util.Optional;

public interface SslContextProvider {

  Optional<SslContext> sslContext();
}
