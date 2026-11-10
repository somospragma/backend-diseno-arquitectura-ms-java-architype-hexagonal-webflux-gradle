package co.com.pragma.accounts.ports;

import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * Puerto de cache. El TTL es obligatorio al escribir (nunca cache eterno).
 */
public interface ICacheGateway {

    Mono<String> get(String key);

    Mono<Boolean> put(String key, String value, Duration ttl);

    Mono<Boolean> evict(String key);
}
