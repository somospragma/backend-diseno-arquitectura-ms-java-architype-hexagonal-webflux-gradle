package co.com.pragma.accounts.infrastructure.kafka;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.ports.IIdempotencyStore;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOffset;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.test.StepVerifier;

class KafkaEventConsumerTest {

    @SuppressWarnings("unchecked")
    private final KafkaReceiver<String, String> receiver = mock(KafkaReceiver.class);

    private static IIdempotencyStore inMemoryStore() {
        java.util.Set<String> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return (key, ttl) -> Mono.just(seen.add(key));
    }

    @SuppressWarnings("unchecked")
    private ReceiverRecord<String, String> record(long offset, ReceiverOffset ro) {
        ReceiverRecord<String, String> r = mock(ReceiverRecord.class);
        when(r.topic()).thenReturn("accounts.events");
        when(r.partition()).thenReturn(0);
        when(r.offset()).thenReturn(offset);
        when(r.receiverOffset()).thenReturn(ro);
        return r;
    }

    @Test
    void shouldAckAfterSuccessfulProcessing() {
        ReceiverOffset ro = mock(ReceiverOffset.class);
        Function<ReceiverRecord<String, String>, Mono<Void>> processor = rec -> Mono.empty();
        KafkaEventConsumer consumer = new KafkaEventConsumer(receiver, processor, inMemoryStore());

        StepVerifier.create(consumer.handleRecordIsolated(record(1L, ro))).verifyComplete();

        verify(ro).acknowledge(); // ack solo tras exito (MSG-O-003)
    }

    @Test
    void shouldNotAckWhenProcessingFails() {
        ReceiverOffset ro = mock(ReceiverOffset.class);
        Function<ReceiverRecord<String, String>, Mono<Void>> processor =
            rec -> Mono.error(new RuntimeException("boom"));
        KafkaEventConsumer consumer = new KafkaEventConsumer(receiver, processor, inMemoryStore());

        // El error se aisla (el stream completa), pero NO se hace ack.
        StepVerifier.create(consumer.handleRecordIsolated(record(2L, ro))).verifyComplete();

        verify(ro, never()).acknowledge();
    }

    @Test
    void shouldSkipDuplicateButAck() {
        ReceiverOffset ro = mock(ReceiverOffset.class);
        int[] processed = {0};
        Function<ReceiverRecord<String, String>, Mono<Void>> processor = rec -> {
            processed[0]++;
            return Mono.empty();
        };
        IIdempotencyStore store = inMemoryStore();
        KafkaEventConsumer consumer = new KafkaEventConsumer(receiver, processor, store);

        StepVerifier.create(consumer.handleRecordIsolated(record(3L, ro))).verifyComplete();
        StepVerifier.create(consumer.handleRecordIsolated(record(3L, ro))).verifyComplete();

        org.junit.jupiter.api.Assertions.assertEquals(1, processed[0]);
        verify(ro, times(2)).acknowledge(); // ambos hacen ack; el duplicado no reprocesa
    }

    @Test
    void consumeShouldProcessRecordsFromReceiver() {
        ReceiverOffset ro = mock(ReceiverOffset.class);
        ReceiverRecord<String, String> rec = record(10L, ro); // construir ANTES de stubear receive()
        when(receiver.receive()).thenReturn(reactor.core.publisher.Flux.just(rec));
        Function<ReceiverRecord<String, String>, Mono<Void>> processor = r -> Mono.empty();
        KafkaEventConsumer consumer = new KafkaEventConsumer(receiver, processor, inMemoryStore());

        StepVerifier.create(consumer.consume()).verifyComplete();
        verify(ro).acknowledge();
    }

    @Test
    void startAndStopShouldManageSubscription() {
        ReceiverOffset ro = mock(ReceiverOffset.class);
        when(receiver.receive())
            .thenReturn(reactor.core.publisher.Flux.<ReceiverRecord<String, String>>never());
        KafkaEventConsumer consumer = new KafkaEventConsumer(receiver, rec -> Mono.empty(), inMemoryStore());

        consumer.start();
        consumer.stop(); // no debe lanzar; dispose de la suscripcion
    }
}
