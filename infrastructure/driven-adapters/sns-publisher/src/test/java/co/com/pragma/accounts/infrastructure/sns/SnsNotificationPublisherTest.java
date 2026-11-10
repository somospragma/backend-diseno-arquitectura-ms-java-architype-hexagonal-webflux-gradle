package co.com.pragma.accounts.infrastructure.sns;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.AccountEvent;
import co.com.pragma.accounts.model.exception.TechnicalException;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sns.SnsAsyncClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

class SnsNotificationPublisherTest {

    private final SnsAsyncClient sns = mock(SnsAsyncClient.class);
    private final SnsProperties props = new SnsProperties("arn:aws:sns:us-east-1:1:topic", "us-east-1");
    private final SnsNotificationPublisher publisher = new SnsNotificationPublisher(sns, props);

    private AccountEvent event() {
        return AccountEvent.builder().accountId("acc-1").eventId("e-1")
            .type("ACCOUNT_CREATED").occurredAt(Instant.now()).build();
    }

    @Test
    void shouldPublishNotification() {
        when(sns.publish(any(PublishRequest.class)))
            .thenReturn(CompletableFuture.completedFuture(PublishResponse.builder().messageId("m-1").build()));

        StepVerifier.create(publisher.notify(event())).verifyComplete();
    }

    @Test
    void shouldMapFailureToTechnicalException() {
        when(sns.publish(any(PublishRequest.class)))
            .thenReturn(CompletableFuture.failedFuture(new RuntimeException("sns down")));

        StepVerifier.create(publisher.notify(event()))
            .expectError(TechnicalException.class)
            .verify();
    }
}
