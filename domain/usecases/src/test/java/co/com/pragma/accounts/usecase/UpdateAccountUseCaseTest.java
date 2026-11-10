package co.com.pragma.accounts.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.ports.IAccountGateway;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class UpdateAccountUseCaseTest {

    @Mock
    private IAccountGateway accountGateway;

    @InjectMocks
    private UpdateAccountUseCase useCase;

    @Test
    void shouldPreserveIdAndCreatedAtOnFullReplace() {
        LocalDateTime created = LocalDateTime.now().minusDays(3);
        Account existing = Account.builder().id("acc-1").holderName("Old").createdAt(created).build();
        Account replacement = Account.builder().holderName("New").status("ACTIVE").build();
        when(accountGateway.findById("acc-1")).thenReturn(Mono.just(existing));
        when(accountGateway.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.execute("acc-1", replacement))
            .assertNext(saved -> {
                org.junit.jupiter.api.Assertions.assertEquals("acc-1", saved.getId());
                org.junit.jupiter.api.Assertions.assertEquals(created, saved.getCreatedAt());
                org.junit.jupiter.api.Assertions.assertEquals("New", saved.getHolderName());
            })
            .verifyComplete();
    }

    @Test
    void shouldFailWithNotFoundWhenMissing() {
        when(accountGateway.findById("missing")).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute("missing", Account.builder().build()))
            .expectError(AccountNotFoundException.class)
            .verify();
    }
}
