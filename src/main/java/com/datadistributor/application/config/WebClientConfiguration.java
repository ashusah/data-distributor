package com.datadistributor.application.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.time.Duration;
import com.datadistributor.application.security.SslContextProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfiguration {

  @Bean
  public WebClient webClient(SslContextProvider sslContextProvider,
                             @Value("${data-distributor.http.connect-timeout-ms:10000}") int connectTimeoutMs,
                             @Value("${data-distributor.http.response-timeout-seconds:10}") long responseTimeoutSec,
                             @Value("${data-distributor.http.read-timeout-seconds:10}") int readTimeoutSec,
                             @Value("${data-distributor.http.write-timeout-seconds:10}") int writeTimeoutSec) {
    HttpClient client = HttpClient.create()
        .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeoutMs)
        .responseTimeout(Duration.ofSeconds(responseTimeoutSec))
        .doOnConnected(conn -> conn
            .addHandlerLast(new ReadTimeoutHandler(readTimeoutSec))
            .addHandlerLast(new WriteTimeoutHandler(writeTimeoutSec)));

    sslContextProvider.sslContext().ifPresent(ctx -> client.secure(spec -> spec.sslContext(ctx)));

    return WebClient.builder()
        .clientConnector(new ReactorClientHttpConnector(client))
        .build();
  }
}
