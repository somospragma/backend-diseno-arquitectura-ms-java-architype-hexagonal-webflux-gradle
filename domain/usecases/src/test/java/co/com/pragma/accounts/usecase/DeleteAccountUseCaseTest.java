package co.com.pragma.accounts.usecase;

import static org.mockito.ArgumentMatchers.anyString;
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
class DeleteAccountUseCaseTest {

    @Mock
    private IAccountGateway accountGateway;

    @Test
    void shouldDeleteWhenAccountExists() {
        when(accountGateway.findById("acc-1"))
            .thenReturn(Mono.just(Account.builder().id("acc-1").build()));
        when(accountGateway.deleteById("acc-1")).thenReturn(Mono.empty());

        StepVerifier.create(new DeleteAccountUseCase(accountGateway).execute("acc-1"))
            .verifyComplete();
    }

    @Test
    void shouldErrorWhenAccountDoesNotExist() {
        when(accountGateway.findById(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(new DeleteAccountUseCase(accountGateway).execute("missing"))
            .expectError(AccountNotFoundException.class)
            .verify();
    }
}
