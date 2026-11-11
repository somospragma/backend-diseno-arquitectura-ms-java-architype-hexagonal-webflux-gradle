package co.com.pragma.accounts.ports;

import co.com.pragma.accounts.model.AccountEvent;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para publicar eventos en un stream (Kafka). La clave de
 * particion preserva el orden por agregado (accountId).
 */
public interface IEventStreamPublisher {
    Mono<Void> publish(AccountEvent event);
}
