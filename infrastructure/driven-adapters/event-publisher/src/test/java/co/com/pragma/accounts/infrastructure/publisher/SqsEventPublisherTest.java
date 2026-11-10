package co.com.pragma.accounts.infrastructure.publisher;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.AccountEvent;
import co.com.pragma.accounts.model.exception.TechnicalException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;

class SqsEventPublisherTest {

    private AccountEvent event() {
        return AccountEvent.builder()
            .accountId("acc-1").eventId("ev-1").type("CREATED")
            .occurredAt(Instant.parse("2026-01-01T00:00:00Z")).build();
    }

    private EventPublisherProperties props() {
        EventPublisherProperties p = new EventPublisherProperties();
        p.setQueueUrl("http://localhost/q");
        return p;
    }

    @Test
    void shouldPublishWithVersionedSchema() {
        SqsAsyncClient sqs = mock(SqsAsyncClient.class);
        ArgumentCaptor<SendMessageRequest> captor = ArgumentCaptor.forClass(SendMessageRequest.class);
        when(sqs.sendMessage(captor.capture()))
            .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().build()));

        StepVerifier.create(new SqsEventPublisher(sqs, props()).publish(event()))
            .verifyComplete();

        SendMessageRequest req = captor.getValue();
        assertTrue(req.messageBody().contains("\"schemaVersion\":\"1\""));
        assertTrue(req.messageAttributes().containsKey("schemaVersion"));
        assertTrue(req.messageAttributes().containsKey("eventType"));
    }

    @Test
    void shouldMapErrorToTechnicalException() {
        SqsAsyncClient sqs = mock(SqsAsyncClient.class);
        when(sqs.sendMessage(any(SendMessageRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("sqs down")));

        StepVerifier.create(new SqsEventPublisher(sqs, props()).publish(event()))
            .expectError(TechnicalException.class)
            .verify();
    }
}
