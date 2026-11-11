package co.com.pragma.accounts.infrastructure.redis;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class RedisCacheAdapterTest {

    @Test
    void putMustRejectMissingTtl() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        RedisCacheAdapter adapter = new RedisCacheAdapter(template);

        StepVerifier.create(adapter.put("k", "v", Duration.ZERO))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void putMustStoreWithTtl() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.set(eq("k"), eq("v"), any(Duration.class))).thenReturn(Mono.just(true));

        RedisCacheAdapter adapter = new RedisCacheAdapter(template);

        StepVerifier.create(adapter.put("k", "v", Duration.ofMinutes(5)))
            .expectNext(true)
            .verifyComplete();
    }

    @Test
    void getMustReturnValueOnHit() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("k")).thenReturn(Mono.just("v"));

        StepVerifier.create(new RedisCacheAdapter(template).get("k"))
            .expectNext("v").verifyComplete();
    }

    @Test
    void evictMustReturnTrueWhenDeleted() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        when(template.delete("k")).thenReturn(Mono.just(1L));

        StepVerifier.create(new RedisCacheAdapter(template).evict("k"))
            .expectNext(true).verifyComplete();
    }

    @Test
    void evictMustDegradeOnError() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        when(template.delete("k")).thenReturn(Mono.error(new RuntimeException("down")));

        StepVerifier.create(new RedisCacheAdapter(template).evict("k"))
            .expectNext(false).verifyComplete();
    }

    @Test
    void putMustDegradeOnError() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.set(any(), any(), any(Duration.class))).thenReturn(Mono.error(new RuntimeException("down")));

        StepVerifier.create(new RedisCacheAdapter(template).put("k", "v", Duration.ofSeconds(1)))
            .expectNext(false).verifyComplete();
    }

    @Test
    void getMustDegradeToMissOnRedisError() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.get("k")).thenReturn(Mono.error(new RuntimeException("redis down")));

        RedisCacheAdapter adapter = new RedisCacheAdapter(template);

        // Degradacion: si Redis falla, get() completa vacio (miss), no propaga error.
        StepVerifier.create(adapter.get("k")).verifyComplete();
    }
}
