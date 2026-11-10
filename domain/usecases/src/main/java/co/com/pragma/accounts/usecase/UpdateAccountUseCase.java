package co.com.pragma.accounts.usecase;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.ports.IAccountGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Reemplazo total de una cuenta existente (HTTP PUT). Idempotente: aplicar la
 * misma peticion N veces deja el mismo estado. Falla con 404 si no existe.
 */
@RequiredArgsConstructor
public class UpdateAccountUseCase {

    private final IAccountGateway accountGateway;

    public Mono<Account> execute(String id, Account replacement) {
        return accountGateway.findById(id)
            .switchIfEmpty(Mono.error(new AccountNotFoundException(id)))
            .flatMap(existing -> {
                replacement.setId(existing.getId());
                replacement.setCreatedAt(existing.getCreatedAt());
                return accountGateway.save(replacement);
            });
    }
}
