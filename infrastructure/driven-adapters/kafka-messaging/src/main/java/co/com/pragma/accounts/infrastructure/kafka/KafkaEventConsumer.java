package co.com.pragma.accounts.infrastructure.kafka;

import co.com.pragma.accounts.ports.IIdempotencyStore;
import java.time.Duration;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.util.retry.Retry;

/**
 * Consumidor Kafka reactivo con AISLAMIENTO DE FALLOS POR REGISTRO (RES-O-003) e
 * IDEMPOTENCIA (MSG-O-001). Reglas:
 *  - Cada registro se procesa aislado con onErrorResume; un fallo individual no
 *    rompe el stream.
 *  - Dedupe por clave del mensaje (o eventId) antes de aplicar el efecto.
 *  - Ack (commit) SOLO tras procesamiento exitoso (MSG-O-003); ante fallo NO se
 *    hace ack, el offset no avanza y el registro se reprocesa (y a DLQ segun la
 *    politica del broker).
 *  - El stream se resuscribe con retry+backoff+jitter si el receive falla.
 */
@RequiredArgsConstructor
public class KafkaEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(KafkaEventConsumer.class);
    private static final Duration DEDUPE_TTL = Duration.ofHours(6);

    private final KafkaReceiver<String, String> kafkaReceiver;
    private final Function<ReceiverRecord<String, String>, Mono<Void>> processor;
    private final IIdempotencyStore idempotencyStore;

    private Disposable subscription;

    public void start() {
        subscription = consume().subscribe();
    }

    public void stop() {
        if (subscription != null) {
            subscription.dispose();
        }
    }

    Flux<Void> consume() {
        return kafkaReceiver.receive()
            .flatMap(this::handleRecordIsolated, 10)
            .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(1))
                .maxBackoff(Duration.ofSeconds(30))
                .jitter(0.5)
                .doBeforeRetry(sig -> log.warn("Kafka consume error, resuscribiendo", sig.failure())));
    }

    Mono<Void> handleRecordIsolated(ReceiverRecord<String, String> record) {
        String key = "kafka:" + record.topic() + ":" + record.partition() + ":" + record.offset();
        return idempotencyStore.markIfFirst(key, DEDUPE_TTL)
            .flatMap(isFirst -> {
                if (Boolean.FALSE.equals(isFirst)) {
                    log.info("Registro Kafka duplicado offset={}; se ignora", record.offset());
                    record.receiverOffset().acknowledge();
                    return Mono.<Void>empty();
                }
                return processor.apply(record)
                    .doOnSuccess(v -> record.receiverOffset().acknowledge()); // ack tras exito (MSG-O-003)
            })
            .onErrorResume(error -> {
                // No se hace ack: el offset no avanza, el registro se reprocesa/DLQ.
                log.error("Error procesando registro Kafka offset={}; NO se hace ack. El consumidor continua.",
                    record.offset(), error);
                return Mono.empty();
            });
    }
}
