package co.com.pragma.accounts.infrastructure.publisher;

import co.com.pragma.accounts.model.AccountEvent;
import co.com.pragma.accounts.model.exception.ErrorCatalog;
import co.com.pragma.accounts.model.exception.TechnicalException;
import co.com.pragma.accounts.ports.IEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * Productor de eventos a SQS. Serializa el evento con un esquema versionado
 * (message attribute schemaVersion, MSG-O-004) e incluye un deduplication id
 * para consumidores idempotentes. Mapea el fallo tecnico sin propagar crudo.
 */
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix = "sqs", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class SqsEventPublisher implements IEventPublisher {

    private static final String SCHEMA_VERSION = "1";

    private final SqsAsyncClient sqsClient;
    private final EventPublisherProperties properties;

    @Override
    public Mono<Void> publish(AccountEvent event) {
        String body = serialize(event);
        SendMessageRequest request = SendMessageRequest.builder()
            .queueUrl(properties.getQueueUrl())
            .messageBody(body)
            .messageAttributes(java.util.Map.of(
                "schemaVersion", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                    .dataType("String").stringValue(SCHEMA_VERSION).build(),
                "eventType", software.amazon.awssdk.services.sqs.model.MessageAttributeValue.builder()
                    .dataType("String").stringValue(event.type()).build()))
            .build();

        return Mono.defer(() -> Mono.fromFuture(sqsClient.sendMessage(request)))
            .then()
            .onErrorMap(e -> new TechnicalException(
                ErrorCatalog.UNEXPECTED_ERROR.code(), "Event publish failed", e));
    }

    private String serialize(AccountEvent event) {
        // Esquema estable y versionado (en un caso real, Jackson/Avro + schema registry).
        return "{\"schemaVersion\":\"" + SCHEMA_VERSION + "\","
            + "\"accountId\":\"" + event.accountId() + "\","
            + "\"eventId\":\"" + event.eventId() + "\","
            + "\"type\":\"" + event.type() + "\","
            + "\"occurredAt\":\"" + event.occurredAt() + "\"}";
    }
}
