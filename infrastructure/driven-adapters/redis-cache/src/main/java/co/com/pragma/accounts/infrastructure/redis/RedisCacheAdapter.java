package co.com.pragma.accounts.infrastructure.redis;

import co.com.pragma.accounts.ports.ICacheGateway;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Cache remoto reactivo con Redis (Lettuce, no bloqueante). Reglas:
 *  - TTL SIEMPRE al escribir (nunca cache eterno).
 *  - DEGRADACION: si Redis falla, el cache no rompe el flujo de negocio
 *    (get -> empty en error; put -> false en error). El caller sigue a la fuente.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisCacheAdapter implements ICacheGateway {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheAdapter.class);

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<String> get(String key) {
        return redisTemplate.opsForValue().get(key)
            .onErrorResume(e -> {
                log.warn("Cache get fallo para {}; se degrada a miss", key, e);
                return Mono.empty();
            });
    }

    @Override
    public Mono<Boolean> put(String key, String value, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return Mono.error(new IllegalArgumentException("TTL is mandatory and must be positive"));
        }
        return redisTemplate.opsForValue().set(key, value, ttl)
            .onErrorResume(e -> {
                log.warn("Cache put fallo para {}; se ignora (degradacion)", key, e);
                return Mono.just(false);
            });
    }

    @Override
    public Mono<Boolean> evict(String key) {
        return redisTemplate.delete(key).map(count -> count > 0)
            .onErrorResume(e -> Mono.just(false));
    }
}
