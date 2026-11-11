package co.com.pragma.accounts.usecase;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.ports.IAccountGateway;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateAccountUseCase {
    private final IAccountGateway accountGateway;

    public Mono<Account> execute(Account account) {
        account.setStatus("ACTIVE");
        account.setCreatedAt(LocalDateTime.now());
        return accountGateway.save(account);
    }
}
