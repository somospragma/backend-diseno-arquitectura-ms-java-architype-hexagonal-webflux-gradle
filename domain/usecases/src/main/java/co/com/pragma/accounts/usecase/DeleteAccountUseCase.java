package co.com.pragma.accounts.usecase;

import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.ports.IAccountGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class DeleteAccountUseCase {
    private final IAccountGateway accountGateway;

    public Mono<Void> execute(String id) {
        return accountGateway.findById(id)
            .switchIfEmpty(Mono.error(new AccountNotFoundException(id)))
            .flatMap(account -> accountGateway.deleteById(account.getId()));
    }
}
