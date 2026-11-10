package co.com.pragma.accounts.infrastructure.creditscore;

import co.com.pragma.accounts.model.CreditScore;
import co.com.pragma.accounts.model.exception.ErrorCatalog;
import co.com.pragma.accounts.model.exception.TechnicalException;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.bulkhead.operator.BulkheadOperator;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Adapter resiliente hacia el servicio externo de score (RES-O-001/002/005).
 * Aplica, de afuera hacia adentro: Retry -> CircuitBreaker -> Bulkhead ->
 * Timeout -> llamada. Nunca propaga el error tecnico crudo: lo mapea a
 * TechnicalException para que el dominio/handler lo trate uniformemente.
 */
@Component
@RequiredArgsConstructor
public class CreditScoreAdapter implements co.com.pragma.accounts.ports.ICreditScoreGateway {

    private static final String INSTANCE = "creditScore";

    private final WebClient creditScoreWebClient;
    private final CreditScoreMapper mapper;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;
    private final BulkheadRegistry bulkheadRegistry;

    @Override
    public Mono<CreditScore> fetchScore(String accountId) {
        CircuitBreaker cb = circuitBreakerRegistry.circuitBreaker(INSTANCE);
        Retry retry = retryRegistry.retry(INSTANCE);
        Bulkhead bulkhead = bulkheadRegistry.bulkhead(INSTANCE);

        return creditScoreWebClient.get()
            .uri("/api/v1/scores/{id}", accountId)
            .retrieve()
            .bodyToMono(CreditScoreDto.class)
            .map(mapper::toModel)
            .timeout(Duration.ofSeconds(3))
            .transformDeferred(BulkheadOperator.of(bulkhead))
            .transformDeferred(CircuitBreakerOperator.of(cb))
            .transformDeferred(RetryOperator.of(retry))
            .onErrorMap(this::isInfrastructureError, e -> new TechnicalException(
                ErrorCatalog.UNEXPECTED_ERROR.code(),
                "Credit score provider call failed", e));
    }

    private boolean isInfrastructureError(Throwable t) {
        return t instanceof TimeoutException
            || t instanceof java.io.IOException
            || t.getClass().getName().startsWith("io.github.resilience4j")
            || t.getClass().getName().startsWith("org.springframework.web.reactive.function.client");
    }
}
