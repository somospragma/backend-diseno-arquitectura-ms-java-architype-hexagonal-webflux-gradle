package co.com.pragma.accounts.infrastructure.sqs;

import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;

class AccountMessageProcessorTest {

    @Test
    void shouldProcessMessageWithoutError() {
        AccountMessageProcessor processor = new AccountMessageProcessor();
        Message message = Message.builder().messageId("m-1").body("{\"event\":\"created\"}").build();

        StepVerifier.create(processor.apply(message)).verifyComplete();
    }
}
