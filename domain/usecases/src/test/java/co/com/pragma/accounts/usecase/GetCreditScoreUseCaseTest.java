package co.com.pragma.accounts.usecase;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.model.CreditScore;
import co.com.pragma.accounts.ports.IAccountGateway;
import co.com.pragma.accounts.ports.ICreditScoreGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class GetCreditScoreUseCaseTest {

    @Mock
    private IAccountGateway accountGateway;
    @Mock
    private ICreditScoreGateway creditScoreGateway;

    @Test
    void shouldReturnScoreWhenAccountExists() {
        when(accountGateway.findById("acc-1"))
            .thenReturn(Mono.just(Account.builder().id("acc-1").build()));
        when(creditScoreGateway.fetchScore("acc-1"))
            .thenReturn(Mono.just(CreditScore.builder().accountId("acc-1").score(700).band("A").build()));

        StepVerifier.create(new GetCreditScoreUseCase(accountGateway, creditScoreGateway).execute("acc-1"))
            .expectNextMatches(s -> s.score() == 700)
            .verifyComplete();
    }

    @Test
    void shouldErrorWhenAccountDoesNotExist() {
        when(accountGateway.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(new GetCreditScoreUseCase(accountGateway, creditScoreGateway).execute("missing"))
            .expectError(AccountNotFoundException.class)
            .verify();
    }
}
