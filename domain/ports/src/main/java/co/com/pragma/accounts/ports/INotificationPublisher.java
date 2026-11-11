package co.com.pragma.accounts.ports;

import co.com.pragma.accounts.model.AccountEvent;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida para publicar notificaciones fan-out (SNS). Separa el
 * concepto de "notificar a multiples suscriptores" del de "encolar trabajo" (SQS).
 */
public interface INotificationPublisher {
    Mono<Void> notify(AccountEvent event);
}
