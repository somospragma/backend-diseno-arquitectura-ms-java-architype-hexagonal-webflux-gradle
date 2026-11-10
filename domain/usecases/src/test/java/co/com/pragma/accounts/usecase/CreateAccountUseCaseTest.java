package co.com.pragma.accounts.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.ports.IAccountGateway;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseTest {

    @Mock
    private IAccountGateway accountGateway;

    @Test
    void shouldSetActiveStatusAndCreatedAtOnCreate() {
        when(accountGateway.save(any(Account.class)))
            .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        CreateAccountUseCase useCase = new CreateAccountUseCase(accountGateway);
        Account input = Account.builder()
            .holderName("Ada Lovelace")
            .accountType("SAVINGS")
            .balance(BigDecimal.TEN)
            .build();

        StepVerifier.create(useCase.execute(input))
            .assertNext(saved -> {
                assertEquals("ACTIVE", saved.getStatus());
                assertNotNull(saved.getCreatedAt());
                assertEquals("Ada Lovelace", saved.getHolderName());
            })
            .verifyComplete();
    }
}
