package co.com.pragma.accounts.infrastructure.redis;

import co.com.pragma.accounts.ports.IIdempotencyStore;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Store de idempotencia sobre Redis (MSG-O-001 / RES-O-004). Usa SETNX + TTL
 * (setIfAbsent) de forma atomica: la primera vez inserta y devuelve true; en
 * reprocesos la clave ya existe y devuelve false. El TTL acota la ventana de
 * deduplicacion para no crecer sin limite.
 */
@Component
@ConditionalOnProperty(prefix = "redis", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class RedisIdempotencyStore implements IIdempotencyStore {

    private final ReactiveStringRedisTemplate redisTemplate;

    @Override
    public Mono<Boolean> markIfFirst(String key, Duration ttl) {
        return redisTemplate.opsForValue().setIfAbsent(key, "1", ttl);
    }
}
