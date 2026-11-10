package co.com.pragma.accounts.infrastructure.sqs;

import co.com.pragma.accounts.ports.IIdempotencyStore;
import java.util.function.Function;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

/**
 * Cablea el consumidor SQS como bean gestionado y lo ARRANCA/DETIENE con el
 * ciclo de vida del contexto (SmartLifecycle). Se activa solo con
 * sqs.consumer.enabled=true, para no exigir infraestructura AWS en el arranque
 * por defecto del arquetipo.
 */
@Configuration
@ConditionalOnProperty(prefix = "sqs.consumer", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(SqsConsumerProperties.class)
public class SqsConsumerConfig {

    @Bean(destroyMethod = "close")
    public SqsAsyncClient sqsAsyncClient(SqsConsumerProperties props) {
        var builder = SqsAsyncClient.builder();
        if (props.region() != null && !props.region().isBlank()) {
            builder.region(Region.of(props.region()));
        }
        return builder.build();
    }

    @Bean
    public SqsMessageConsumer sqsMessageConsumer(SqsAsyncClient sqsAsyncClient,
                                                 SqsConsumerProperties props,
                                                 Function<Message, Mono<Void>> processor,
                                                 IIdempotencyStore idempotencyStore) {
        return new SqsMessageConsumer(sqsAsyncClient, props.queueUrl(), processor, idempotencyStore);
    }

    /** Arranca el poll loop al iniciar el contexto y lo detiene al cerrarlo. */
    @Bean
    public SmartLifecycle sqsConsumerLifecycle(SqsMessageConsumer consumer) {
        return new SmartLifecycle() {
            private volatile boolean running;

            @Override
            public void start() {
                consumer.start();
                running = true;
            }

            @Override
            public void stop() {
                consumer.stop();
                running = false;
            }

            @Override
            public boolean isRunning() {
                return running;
            }
        };
    }
}
