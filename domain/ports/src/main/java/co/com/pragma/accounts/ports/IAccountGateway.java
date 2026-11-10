package co.com.pragma.accounts.ports;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountSearchCriteria;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IAccountGateway {
    Mono<Account> save(Account account);

    Mono<Account> findById(String id);

    Flux<Account> findAll(int limit);

    Mono<Void> deleteById(String id);

    Mono<Boolean> existsById(String id);

    /** Busqueda por criterios (usada por el metodo HTTP QUERY). */
    Flux<Account> search(AccountSearchCriteria criteria, int limit);
}
