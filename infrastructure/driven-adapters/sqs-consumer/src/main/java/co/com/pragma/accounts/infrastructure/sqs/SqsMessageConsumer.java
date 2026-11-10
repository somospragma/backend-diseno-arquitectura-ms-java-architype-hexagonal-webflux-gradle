package co.com.pragma.accounts.infrastructure.sqs;

import co.com.pragma.accounts.ports.IIdempotencyStore;
import java.time.Duration;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * Consumidor SQS reactivo con AISLAMIENTO DE FALLOS POR MENSAJE (RES-O-003).
 *
 * Regla central: una excepcion procesando UN mensaje NUNCA rompe el consumidor.
 *  - Cada mensaje se procesa en su propio flatMap con onErrorResume.
 *  - El Flux principal jamas emite onError por un mensaje individual.
 *  - Un mensaje que falla NO se borra: vuelve a la cola y, tras maxReceiveCount,
 *    la redrive policy de SQS lo envia a la DLQ (MSG-O-002/003).
 *  - El stream de polling se resuscribe con retry+backoff si el receive falla.
 */
@RequiredArgsConstructor
public class SqsMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(SqsMessageConsumer.class);

    private static final Duration DEDUPE_TTL = Duration.ofHours(6);

    private final SqsAsyncClient sqsClient;
    private final String queueUrl;
    private final Function<Message, Mono<Void>> processor;
    /** Deduplicacion at-least-once por messageId (MSG-O-001). */
    private final IIdempotencyStore idempotencyStore;

    private Disposable subscription;

    public void start() {
        subscription = pollLoop().subscribe();
    }

    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    Flux<Void> pollLoop() {
        return receiveBatch()
            .flatMap(this::handleMessageIsolated, 10) // concurrencia controlada
            .repeat()                                  // siguiente batch
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(30))
                .jitter(0.5)
                .doBeforeRetry(sig -> log.warn("SQS poll loop error, resuscribiendo", sig.failure())));
    }

    /** Visible para test: un solo batch, sin el repeat/retry del loop. */
    Flux<Message> receiveBatchForTest() {
        return receiveBatch();
    }

    private Flux<Message> receiveBatch() {
        ReceiveMessageRequest request = ReceiveMessageRequest.builder()
            .queueUrl(queueUrl)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(20)   // long polling
            .build();
        return Mono.fromFuture(sqsClient.receiveMessage(request))
            .flatMapMany(resp -> Flux.fromIterable(resp.messages()));
    }

    /**
     * Procesa un mensaje AISLADO: si falla, se registra y el mensaje NO se borra
     * (ira a DLQ por reintentos). El error se contiene aqui con onErrorResume;
     * jamas se propaga al Flux principal.
     */
    Mono<Void> handleMessageIsolated(Message message) {
        // Idempotencia (MSG-O-001): si el messageId ya se proceso, se salta el
        // efecto pero igual se borra (ya no hay nada que hacer con el duplicado).
        return idempotencyStore.markIfFirst("sqs:" + message.messageId(), DEDUPE_TTL)
            .flatMap(isFirst -> {
                if (Boolean.FALSE.equals(isFirst)) {
                    log.info("Mensaje {} duplicado (ya procesado); se ignora y se borra", message.messageId());
                    return deleteMessage(message);
                }
                return processor.apply(message)
                    .then(deleteMessage(message)); // solo se borra si el proceso fue exitoso (MSG-O-003)
            })
            .onErrorResume(error -> {
                log.error("Error procesando mensaje {}; NO se borra (ira a DLQ por reintentos). El consumidor continua.",
                    message.messageId(), error);
                return Mono.empty(); // aisla el fallo: el resto de mensajes sigue
            });
    }

    private Mono<Void> deleteMessage(Message message) {
        // defer: el future del SDK se crea SOLO al suscribir, no al construir el
        // pipeline. Si no, el delete se ejecutaria aunque el processor falle.
        return Mono.defer(() -> Mono.fromFuture(sqsClient.deleteMessage(DeleteMessageRequest.builder()
                .queueUrl(queueUrl)
                .receiptHandle(message.receiptHandle())
                .build())))
            .then();
    }
}
