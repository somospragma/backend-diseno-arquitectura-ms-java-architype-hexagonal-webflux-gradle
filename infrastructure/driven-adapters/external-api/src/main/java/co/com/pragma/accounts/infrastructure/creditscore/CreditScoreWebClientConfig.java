package co.com.pragma.accounts.infrastructure.creditscore;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * WebClient del servicio de score con timeouts de transporte (ultima linea de
 * defensa, RES-O-001) y pool de conexiones reutilizado (PERF-O-003).
 */
@Configuration
public class CreditScoreWebClientConfig {

    @Bean
    public WebClient creditScoreWebClient(
            @Value("${credit-score.base-url:https://credit-score.internal}") String baseUrl) {

        ConnectionProvider provider = ConnectionProvider.builder("credit-score")
            .maxConnections(50)
            .pendingAcquireTimeout(Duration.ofSeconds(5))
            .build();

        HttpClient httpClient = HttpClient.create(provider)
            .responseTimeout(Duration.ofSeconds(3))
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
            .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(3, TimeUnit.SECONDS)));

        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
