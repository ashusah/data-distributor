package com.datadistributor.application.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import com.datadistributor.application.security.SslContextProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Configures the shared reactive WebClient with timeouts and optional SSL context.
 */
@Configuration
public class WebClientConfiguration {

  @Bean
  public WebClient webClient(SslContextProvider sslContextProvider, DataDistributorProperties properties) {
    var http = properties.getHttp();
    HttpClient client = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, http.getConnectTimeoutMs())
        .responseTimeout(Duration.ofSeconds(http.getResponseTimeoutSeconds()))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(http.getReadTimeoutSeconds()))
            .addHandlerLast(new WriteTimeoutHandler(http.getWriteTimeoutSeconds())));

    sslContextProvider.sslContext().ifPresent(ctx -> client.secure(spec -> spec.sslContext(ctx)));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(client))
        .build();
  }
}
