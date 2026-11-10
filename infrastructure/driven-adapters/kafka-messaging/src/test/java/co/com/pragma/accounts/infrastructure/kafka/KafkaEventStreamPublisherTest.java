package co.com.pragma.accounts.infrastructure.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.AccountEvent;
import co.com.pragma.accounts.model.exception.TechnicalException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderResult;
import reactor.test.StepVerifier;

class KafkaEventStreamPublisherTest {

    @SuppressWarnings("unchecked")
    private final KafkaSender<String, String> sender = mock(KafkaSender.class);
    private final KafkaEventStreamPublisher publisher = new KafkaEventStreamPublisher(sender, "accounts.events");

    private AccountEvent event() {
        return AccountEvent.builder().accountId("acc-1").eventId("e-1")
            .type("ACCOUNT_CREATED").occurredAt(Instant.now()).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldPublishToKafka() {
        SenderResult<String> result = mock(SenderResult.class);
        when(sender.send(any(Publisher.class))).thenReturn(Flux.just(result));

        StepVerifier.create(publisher.publish(event())).verifyComplete();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMapFailureToTechnicalException() {
        when(sender.send(any(Publisher.class)))
            .thenReturn(Flux.error(new RuntimeException("kafka down")));

        StepVerifier.create(publisher.publish(event()))
            .expectError(TechnicalException.class)
            .verify();
    }
}
