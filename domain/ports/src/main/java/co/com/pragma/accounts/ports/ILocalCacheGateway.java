package co.com.pragma.accounts.ports;

import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * Cache LOCAL en proceso (Caffeine), complementaria al cache remoto (Redis).
 * Sirve como primer nivel de baja latencia (near-cache). El TTL es obligatorio
 * al escribir (nunca cache eterno) y el tamano esta acotado (PERF-BP-005).
 */
public interface ILocalCacheGateway {

    Mono<String> get(String key);

    Mono<Void> put(String key, String value, Duration ttl);

    Mono<Void> evict(String key);
}
