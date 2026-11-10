package co.com.pragma.accounts.infrastructure.kafka;

import co.com.pragma.accounts.model.AccountEvent;
import co.com.pragma.accounts.model.exception.ErrorCatalog;
import co.com.pragma.accounts.model.exception.TechnicalException;
import co.com.pragma.accounts.ports.IEventStreamPublisher;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderRecord;

/**
 * Productor de eventos a Kafka con reactor-kafka. Usa accountId como clave de
 * particion (preserva orden por agregado). Serializa con esquema versionado y
 * traduce el fallo tecnico. El header schemaVersion documenta el contrato (MSG-O-004).
 */
@RequiredArgsConstructor
public class KafkaEventStreamPublisher implements IEventStreamPublisher {

    private static final String SCHEMA_VERSION = "1";

    private final KafkaSender<String, String> kafkaSender;
    private final String topic;

    @Override
    public Mono<Void> publish(AccountEvent event) {
        ProducerRecord<String, String> record =
            new ProducerRecord<>(topic, event.accountId(), serialize(event));
        record.headers().add("schemaVersion", SCHEMA_VERSION.getBytes());
        record.headers().add("eventType", event.type().getBytes());

        return kafkaSender.send(Mono.just(SenderRecord.create(record, event.eventId())))
            .then()
            .onErrorMap(e -> new TechnicalException(
                ErrorCatalog.UNEXPECTED_ERROR.code(), "Kafka publish failed", e));
    }

    private String serialize(AccountEvent event) {
        return "{\"schemaVersion\":\"" + SCHEMA_VERSION + "\","
            + "\"accountId\":\"" + event.accountId() + "\","
            + "\"eventId\":\"" + event.eventId() + "\","
            + "\"type\":\"" + event.type() + "\","
            + "\"occurredAt\":\"" + event.occurredAt() + "\"}";
    }
}
