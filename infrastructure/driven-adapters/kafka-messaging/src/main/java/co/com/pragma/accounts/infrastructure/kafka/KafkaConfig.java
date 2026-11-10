package co.com.pragma.accounts.infrastructure.kafka;

import co.com.pragma.accounts.ports.IIdempotencyStore;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import reactor.kafka.receiver.KafkaReceiver;
import reactor.kafka.receiver.ReceiverOptions;
import reactor.kafka.receiver.ReceiverRecord;
import reactor.kafka.sender.KafkaSender;
import reactor.kafka.sender.SenderOptions;

/**
 * Wiring del adapter Kafka reactivo (productor + consumidor). Solo activo con
 * kafka.enabled=true. El consumidor se arranca/detiene con el ciclo de vida del
 * contexto (SmartLifecycle) y hace commit manual (ack tras exito).
 */
@Configuration
@ConditionalOnProperty(prefix = "kafka", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfig {

    @Bean
    public KafkaSender<String, String> kafkaSender(KafkaProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");                 // durabilidad
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);    // no duplicar en reintentos
        return KafkaSender.create(SenderOptions.create(config));
    }

    @Bean
    public KafkaEventStreamPublisher kafkaEventStreamPublisher(KafkaSender<String, String> sender,
                                                               KafkaProperties props) {
        return new KafkaEventStreamPublisher(sender, props.topic());
    }

    @Bean
    public KafkaReceiver<String, String> kafkaReceiver(KafkaProperties props) {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, props.bootstrapServers());
        config.put(ConsumerConfig.GROUP_ID_CONFIG, props.consumerGroup());
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);   // commit manual tras exito
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        ReceiverOptions<String, String> options = ReceiverOptions.<String, String>create(config)
            .subscription(Collections.singletonList(props.topic()));
        return KafkaReceiver.create(options);
    }

    @Bean
    public KafkaEventConsumer kafkaEventConsumer(
            KafkaReceiver<String, String> receiver,
            Function<ReceiverRecord<String, String>, Mono<Void>> processor,
            IIdempotencyStore idempotencyStore) {
        return new KafkaEventConsumer(receiver, processor, idempotencyStore);
    }

    @Bean
    public SmartLifecycle kafkaConsumerLifecycle(KafkaEventConsumer consumer) {
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
