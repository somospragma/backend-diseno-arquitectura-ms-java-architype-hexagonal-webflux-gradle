package co.com.pragma.accounts.usecase;

import co.com.pragma.accounts.model.CreditScore;
import co.com.pragma.accounts.ports.IAccountGateway;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.ports.ICreditScoreGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

/**
 * Consulta el score de una cuenta existente combinando el gateway de cuentas
 * (verifica que exista) con el gateway externo de score (resiliente).
 */
@RequiredArgsConstructor
public class GetCreditScoreUseCase {

    private final IAccountGateway accountGateway;
    private final ICreditScoreGateway creditScoreGateway;

    public Mono<CreditScore> execute(String accountId) {
        return accountGateway.findById(accountId)
            .switchIfEmpty(Mono.error(new AccountNotFoundException(accountId)))
            .flatMap(account -> creditScoreGateway.fetchScore(account.getId()));
    }
}
