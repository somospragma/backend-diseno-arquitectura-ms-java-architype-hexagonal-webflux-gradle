package co.com.pragma.accounts.infrastructure.caffeine;

import co.com.pragma.accounts.ports.ILocalCacheGateway;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.time.Instant;
import reactor.core.publisher.Mono;

/**
 * Cache LOCAL en proceso con Caffeine (PERF-BP-005), near-cache de baja latencia.
 * Reglas:
 *  - TTL obligatorio y positivo al escribir (nunca cache eterno).
 *  - Tamano acotado por maximumSize (Caffeine evicta por tamano) y expireAfterWrite
 *    global como tope de seguridad.
 *  - TTL POR ENTRADA: como el expireAfterWrite de Caffeine es global, se guarda el
 *    instante de expiracion junto al valor y se valida en la lectura, de modo que
 *    cada put respeta su propio TTL.
 */
public class CaffeineLocalCacheAdapter implements ILocalCacheGateway {

    /** Valor + expiracion para soportar TTL por entrada. */
    private record Entry(String value, Instant expiresAt) {
        boolean expired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Cache<String, Entry> cache;

    public CaffeineLocalCacheAdapter(long maximumSize, long maxExpireAfterSeconds) {
        this.cache = Caffeine.newBuilder()
            .maximumSize(maximumSize)
            .expireAfterWrite(Duration.ofSeconds(maxExpireAfterSeconds))
            .build();
    }

    @Override
    public Mono<String> get(String key) {
        return Mono.fromSupplier(() -> {
            Entry entry = cache.getIfPresent(key);
            if (entry == null) {
                return null;
            }
            if (entry.expired()) {
                cache.invalidate(key);
                return null;
            }
            return entry.value();
        });
    }

    @Override
    public Mono<Void> put(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Mono.error(new IllegalArgumentException("TTL is mandatory and must be positive"));
        }
        return Mono.fromRunnable(() ->
            cache.put(key, new Entry(value, Instant.now().plus(ttl))));
    }

    @Override
    public Mono<Void> evict(String key) {
        return Mono.fromRunnable(() -> cache.invalidate(key));
    }
}
