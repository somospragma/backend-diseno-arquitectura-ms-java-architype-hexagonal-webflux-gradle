package co.com.pragma.accounts.infrastructure.caffeine;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class CaffeineLocalCacheAdapterTest {

    private final CaffeineLocalCacheAdapter cache = new CaffeineLocalCacheAdapter(100, 300);

    @Test
    void getMissReturnsEmpty() {
        StepVerifier.create(cache.get("absent")).verifyComplete(); // Mono vacio
    }

    @Test
    void putThenGetReturnsValue() {
        StepVerifier.create(cache.put("k", "v", Duration.ofMinutes(1))).verifyComplete();
        StepVerifier.create(cache.get("k")).expectNext("v").verifyComplete();
    }

    @Test
    void putWithoutTtlIsRejected() {
        StepVerifier.create(cache.put("k", "v", Duration.ZERO))
            .expectError(IllegalArgumentException.class)
            .verify();
    }

    @Test
    void expiredEntryReturnsEmpty() throws InterruptedException {
        StepVerifier.create(cache.put("k", "v", Duration.ofMillis(10))).verifyComplete();
        Thread.sleep(30);
        StepVerifier.create(cache.get("k")).verifyComplete(); // expiro -> vacio
    }

    @Test
    void evictRemovesEntry() {
        StepVerifier.create(cache.put("k", "v", Duration.ofMinutes(1))).verifyComplete();
        StepVerifier.create(cache.evict("k")).verifyComplete();
        StepVerifier.create(cache.get("k")).verifyComplete();
    }
}
