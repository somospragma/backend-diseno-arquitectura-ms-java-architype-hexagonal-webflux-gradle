package co.com.pragma.accounts.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.ports.IAccountGateway;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class PatchAccountUseCaseTest {

    @Mock
    private IAccountGateway accountGateway;

    @InjectMocks
    private PatchAccountUseCase useCase;

    @Test
    void shouldApplyOnlyPresentFields() {
        Account existing = Account.builder()
            .id("acc-1").holderName("Ada").email("ada@pragma.co")
            .accountType("SAVINGS").balance(BigDecimal.TEN).status("ACTIVE").build();
        Account patch = Account.builder().status("FROZEN").build();
        when(accountGateway.findById("acc-1")).thenReturn(Mono.just(existing));
        when(accountGateway.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.execute("acc-1", patch))
            .assertNext(saved -> {
                // Solo status cambia; el resto se conserva.
                org.junit.jupiter.api.Assertions.assertEquals("FROZEN", saved.getStatus());
                org.junit.jupiter.api.Assertions.assertEquals("Ada", saved.getHolderName());
                org.junit.jupiter.api.Assertions.assertEquals(BigDecimal.TEN, saved.getBalance());
            })
            .verifyComplete();
    }

    @Test
    void shouldApplyBalanceAndOtherFields() {
        Account existing = Account.builder().id("acc-1").holderName("Ada").balance(BigDecimal.ZERO).build();
        Account patch = Account.builder().holderName("Ada L.").balance(BigDecimal.valueOf(50)).build();
        when(accountGateway.findById("acc-1")).thenReturn(Mono.just(existing));
        when(accountGateway.save(any(Account.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        StepVerifier.create(useCase.execute("acc-1", patch))
            .assertNext(saved -> {
                org.junit.jupiter.api.Assertions.assertEquals("Ada L.", saved.getHolderName());
                org.junit.jupiter.api.Assertions.assertEquals(BigDecimal.valueOf(50), saved.getBalance());
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
