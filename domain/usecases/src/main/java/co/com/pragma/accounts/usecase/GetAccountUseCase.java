package co.com.pragma.accounts.usecase;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.ports.IAccountGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetAccountUseCase {
    private final IAccountGateway accountGateway;

    public Mono<Account> execute(String id) {
        return accountGateway.findById(id)
            .switchIfEmpty(Mono.error(new AccountNotFoundException(id)));
    }

    public Flux<Account> findAll(int limit) {
        return accountGateway.findAll(limit);
    }
}
