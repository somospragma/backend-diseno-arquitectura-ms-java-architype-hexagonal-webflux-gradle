package co.com.pragma.accounts.infrastructure.sqs;

import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Procesador de negocio de un mensaje SQS. Aqui iria la deserializacion del
 * body (validando el esquema versionado, MSG-O-004) y la invocacion del use case
 * correspondiente. En el arquetipo solo registra el mensaje como ejemplo.
 */
@Component
public class AccountMessageProcessor implements Function<Message, Mono<Void>> {

    private static final Logger log = LoggerFactory.getLogger(AccountMessageProcessor.class);

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromRunnable(() ->
            log.info("Procesando mensaje SQS id={} body={}", message.messageId(), message.body()));
    }
}
