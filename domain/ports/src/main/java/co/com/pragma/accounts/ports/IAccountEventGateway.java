package co.com.pragma.accounts.ports;

import co.com.pragma.accounts.model.AccountEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida hacia el store de eventos por cuenta (DynamoDB).
 * La consulta es SIEMPRE por clave (accountId), nunca un barrido completo.
 */
public interface IAccountEventGateway {

    Mono<Void> append(AccountEvent event);

    /** Lista los eventos de una cuenta por Query sobre la PK, con tope. */
    Flux<AccountEvent> findByAccountId(String accountId, int limit);
}
