package co.com.pragma.accounts;

import java.time.Duration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import reactor.blockhound.BlockHound;
import reactor.blockhound.BlockingOperationError;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * BlockHound detecta llamadas bloqueantes en threads non-blocking (event loop
 * de Netty / Schedulers.parallel). Debe estar activo en CI.
 */
class BlockHoundTest {

    @BeforeAll
    static void setUp() {
        BlockHound.install();
    }

    @Test
    void shouldDetectBlockingCallInNonBlockingThread() {
        Mono<Long> blockingMono = Mono.delay(Duration.ofMillis(1))
            .doOnNext(it -> {
                try {
                    Thread.sleep(10); // llamada bloqueante deliberada
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

        Assertions.assertThrows(Throwable.class, () -> {
            try {
                blockingMono.subscribeOn(Schedulers.parallel()).block();
            } catch (Throwable t) {
                if (t instanceof BlockingOperationError
                    || (t.getCause() instanceof BlockingOperationError)) {
                    throw t;
                }
                throw t;
            }
        });
    }
}
