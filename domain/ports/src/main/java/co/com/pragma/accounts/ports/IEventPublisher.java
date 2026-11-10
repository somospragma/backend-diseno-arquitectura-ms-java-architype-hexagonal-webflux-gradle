package co.com.pragma.accounts.ports;

import co.com.pragma.accounts.model.AccountEvent;
import reactor.core.publisher.Mono;

/** Puerto de salida para publicar eventos de dominio. */
public interface IEventPublisher {
    Mono<Void> publish(AccountEvent event);
}
