package co.com.pragma.accounts.infrastructure.kafka;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.test.StepVerifier;

class KafkaRecordProcessorTest {

    @Test
    @SuppressWarnings("unchecked")
    void shouldProcessRecordWithoutError() {
        KafkaRecordProcessor processor = new KafkaRecordProcessor();
        ReceiverRecord<String, String> record = mock(ReceiverRecord.class);
        when(record.key()).thenReturn("acc-1");
        when(record.value()).thenReturn("{\"event\":\"created\"}");

        StepVerifier.create(processor.apply(record)).verifyComplete();
    }
}
