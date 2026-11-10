package co.com.pragma.accounts.usecase;

import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.ports.IAccountGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetAccountUseCaseTest {

    @Mock
    private IAccountGateway accountGateway;

    @Test
    void shouldReturnAccountWhenExists() {
        Account account = Account.builder().id("acc-1").status("ACTIVE").build();
        when(accountGateway.findById("acc-1")).thenReturn(Mono.just(account));

        GetAccountUseCase useCase = new GetAccountUseCase(accountGateway);

        StepVerifier.create(useCase.execute("acc-1"))
            .expectNextMatches(result -> result.getId().equals("acc-1"))
            .verifyComplete();
    }

    @Test
    void shouldErrorWhenAccountDoesNotExist() {
        when(accountGateway.findById("acc-404")).thenReturn(Mono.empty());

        GetAccountUseCase useCase = new GetAccountUseCase(accountGateway);

        StepVerifier.create(useCase.execute("acc-404"))
            .expectError(AccountNotFoundException.class)
            .verify();
    }

    @Test
    void shouldListAllWithLimit() {
        when(accountGateway.findAll(10)).thenReturn(reactor.core.publisher.Flux.just(
            Account.builder().id("a").build(), Account.builder().id("b").build()));

        StepVerifier.create(new GetAccountUseCase(accountGateway).findAll(10))
            .expectNextCount(2)
            .verifyComplete();
    }
}
