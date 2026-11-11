package co.com.pragma.accounts.infrastructure.sqs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;

class SqsMessageConsumerTest {

    private Message msg(String id, String body) {
        return Message.builder().messageId(id).receiptHandle("rh-" + id).body(body).build();
    }

    /** Store en memoria: markIfFirst devuelve true solo la primera vez por clave. */
    private static co.com.pragma.accounts.ports.IIdempotencyStore inMemoryStore() {
        java.util.Set<String> seen = java.util.concurrent.ConcurrentHashMap.newKeySet();
        return (key, ttl) -> Mono.just(seen.add(key));
    }

    @Test
    void poisonMessageMustNotStopProcessingOfOthers() {
        SqsAsyncClient sqs = mock(SqsAsyncClient.class);
        ConcurrentLinkedQueue<String> deleted = new ConcurrentLinkedQueue<>();
        when(sqs.deleteMessage(any(DeleteMessageRequest.class))).thenAnswer(inv -> {
            DeleteMessageRequest r = inv.getArgument(0);
            deleted.add(r.receiptHandle());
            return CompletableFuture.completedFuture(DeleteMessageResponse.builder().build());
        });

        AtomicInteger processed = new AtomicInteger();
        // El mensaje "poison" lanza; los demas se procesan bien.
        Function<Message, Mono<Void>> processor = m -> {
            if ("poison".equals(m.body())) {
                return Mono.error(new RuntimeException("boom en un mensaje"));
            }
            processed.incrementAndGet();
            return Mono.empty();
        };

        SqsMessageConsumer consumer = new SqsMessageConsumer(sqs, "queue-url", processor, inMemoryStore());

        // Procesamos un batch de 3: ok, poison, ok
        StepVerifier.create(
                reactor.core.publisher.Flux.just(msg("1", "ok"), msg("2", "poison"), msg("3", "ok"))
                    .flatMap(consumer::handleMessageIsolated))
            .verifyComplete(); // el flujo COMPLETA pese al mensaje veneno

        assertEquals(2, processed.get(), "los mensajes sanos se procesaron");
        // solo los sanos se borran; el veneno NO se borra (ira a DLQ por reintentos)
        assertEquals(2, deleted.size());
        assertTrue(deleted.contains("rh-1") && deleted.contains("rh-3"));
    }

    @Test
    void pollLoopMustReceiveAndProcessBatch() {
        SqsAsyncClient sqs = mock(SqsAsyncClient.class);
        AtomicInteger processed = new AtomicInteger();
        when(sqs.receiveMessage(any(software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse.builder()
                    .messages(msg("1", "ok"), msg("2", "ok")).build()));
        when(sqs.deleteMessage(any(DeleteMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(DeleteMessageResponse.builder().build()));

        SqsMessageConsumer consumer = new SqsMessageConsumer(sqs, "q", m -> {
            processed.incrementAndGet();
            return Mono.<Void>empty();
        }, inMemoryStore());

        // Tomamos solo el primer batch (receiveBatch) para no repetir infinito.
        StepVerifier.create(consumer.receiveBatchForTest().flatMap(consumer::handleMessageIsolated))
            .verifyComplete();
        assertEquals(2, processed.get());
    }

    @Test
    void receiveBatchMustEmitReceivedMessages() {
        SqsAsyncClient sqs = mock(SqsAsyncClient.class);
        when(sqs.receiveMessage(any(software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(
                software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse.builder()
                    .messages(msg("1", "ok"), msg("2", "ok")).build()));

        SqsMessageConsumer consumer = new SqsMessageConsumer(sqs, "q", m -> Mono.empty(), inMemoryStore());

        StepVerifier.create(consumer.receiveBatchForTest())
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void successfulMessageMustBeDeleted() {
        SqsAsyncClient sqs = mock(SqsAsyncClient.class);
        ConcurrentLinkedQueue<String> deleted = new ConcurrentLinkedQueue<>();
        when(sqs.deleteMessage(any(DeleteMessageRequest.class))).thenAnswer(inv -> {
            deleted.add(((DeleteMessageRequest) inv.getArgument(0)).receiptHandle());
            return CompletableFuture.completedFuture(DeleteMessageResponse.builder().build());
        });

        SqsMessageConsumer consumer = new SqsMessageConsumer(sqs, "q", m -> Mono.empty(), inMemoryStore());

        StepVerifier.create(consumer.handleMessageIsolated(msg("1", "ok"))).verifyComplete();
        assertEquals(List.of("rh-1"), List.copyOf(deleted));
    }

    @Test
    void duplicateMessageMustBeSkippedButDeleted() {
        SqsAsyncClient sqs = mock(SqsAsyncClient.class);
        ConcurrentLinkedQueue<String> deleted = new ConcurrentLinkedQueue<>();
        when(sqs.deleteMessage(any(DeleteMessageRequest.class))).thenAnswer(inv -> {
            deleted.add(((DeleteMessageRequest) inv.getArgument(0)).receiptHandle());
            return CompletableFuture.completedFuture(DeleteMessageResponse.builder().build());
        });
        AtomicInteger processed = new AtomicInteger();
        // Store compartido: la primera vez marca, la segunda ya existe -> duplicado.
        co.com.pragma.accounts.ports.IIdempotencyStore store = inMemoryStore();

        SqsMessageConsumer consumer = new SqsMessageConsumer(sqs, "q", m -> {
            processed.incrementAndGet();
            return Mono.empty();
        }, store);

        // Mismo messageId dos veces (entrega at-least-once).
        StepVerifier.create(consumer.handleMessageIsolated(msg("dup", "ok"))).verifyComplete();
        StepVerifier.create(consumer.handleMessageIsolated(msg("dup", "ok"))).verifyComplete();

        assertEquals(1, processed.get(), "el efecto de negocio se aplica una sola vez");
        assertEquals(2, deleted.size(), "ambos se borran (el duplicado tambien)");
    }
}
