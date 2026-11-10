package co.com.pragma.accounts.infrastructure.kafka;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.ReceiverRecord;

/**
 * Procesador de negocio de un registro Kafka. Aqui iria la deserializacion
 * (validando el esquema versionado) y la invocacion del use case. En el
 * arquetipo solo registra el registro como ejemplo.
 */
@Component
public class KafkaRecordProcessor implements Function<ReceiverRecord<String, String>, Mono<Void>> {

    private static final Logger log = LoggerFactory.getLogger(KafkaRecordProcessor.class);

    @Override
    public Mono<Void> apply(ReceiverRecord<String, String> record) {
        return Mono.fromRunnable(() ->
            log.info("Procesando registro Kafka key={} value={}", record.key(), record.value()));
    }
}
