package co.com.pragma.accounts.infrastructure.creditscore;

import static org.junit.jupiter.api.Assertions.assertEquals;

import co.com.pragma.accounts.model.exception.TechnicalException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CreditScoreAdapterTest {

    private final CreditScoreMapper mapper = Mappers.getMapper(CreditScoreMapper.class);
    private final CircuitBreakerRegistry cbRegistry = CircuitBreakerRegistry.ofDefaults();
    private final BulkheadRegistry bhRegistry = BulkheadRegistry.ofDefaults();
    private final RetryRegistry retryRegistry = RetryRegistry.of(
        RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofMillis(10))
            .retryExceptions(IOException.class)
            .build());

    private CreditScoreAdapter adapterWith(WebClient webClient) {
        return new CreditScoreAdapter(webClient, mapper, cbRegistry, retryRegistry, bhRegistry);
    }

    @Test
    void shouldReturnScoreOnSuccess() {
        WebClient webClient = WebClient.builder()
            .exchangeFunction(req -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body("{\"accountId\":\"acc-1\",\"score\":720,\"band\":\"A\"}")
                .build()))
            .build();

        StepVerifier.create(adapterWith(webClient).fetchScore("acc-1"))
            .assertNext(s -> {
                assertEquals("acc-1", s.accountId());
                assertEquals(720, s.score());
                assertEquals("A", s.band());
            })
            .verifyComplete();
    }

    @Test
    void shouldRetryOnTransientErrorThenSucceed() {
        AtomicInteger calls = new AtomicInteger();
        WebClient webClient = WebClient.builder()
            .exchangeFunction(req -> {
                if (calls.incrementAndGet() < 3) {
                    return Mono.error(new IOException("transient network error"));
                }
                return Mono.just(ClientResponse.create(HttpStatus.OK)
                    .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                    .body("{\"accountId\":\"acc-2\",\"score\":650,\"band\":\"B\"}")
                    .build());
            })
            .build();

        StepVerifier.create(adapterWith(webClient).fetchScore("acc-2"))
            .assertNext(s -> assertEquals(650, s.score()))
            .verifyComplete();
        assertEquals(3, calls.get());
    }

    @Test
    void shouldMapToTechnicalExceptionWhenAllRetriesFail() {
        WebClient webClient = WebClient.builder()
            .exchangeFunction(req -> Mono.error(new IOException("dependency down")))
            .build();

        StepVerifier.create(adapterWith(webClient).fetchScore("acc-3"))
            .expectError(TechnicalException.class)
            .verify();
    }
}
