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

class RedisIdempotencyStoreTest {

    @Test
    @SuppressWarnings("unchecked")
    void markIfFirstDelegatesToSetIfAbsentWithTtl() {
        ReactiveStringRedisTemplate template = mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> ops = mock(ReactiveValueOperations.class);
        when(template.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(eq("k"), any(), eq(Duration.ofHours(1)))).thenReturn(Mono.just(true));

        RedisIdempotencyStore store = new RedisIdempotencyStore(template);

        StepVerifier.create(store.markIfFirst("k", Duration.ofHours(1)))
            .expectNext(true)
            .verifyComplete();
    }
}
