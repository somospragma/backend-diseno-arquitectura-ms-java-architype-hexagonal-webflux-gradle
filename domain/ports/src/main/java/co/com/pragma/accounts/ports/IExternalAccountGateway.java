package co.com.pragma.accounts.ports;

import co.com.pragma.accounts.model.Account;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida hacia OTRO microservicio de cuentas via gRPC (comunicacion
 * sincrona interna, MSG-BP-006). Retorna el modelo de dominio; el transporte
 * gRPC queda encapsulado en el adapter.
 */
public interface IExternalAccountGateway {

    Mono<Account> getAccount(String id);

    Flux<Account> listAccounts(int limit);
}
