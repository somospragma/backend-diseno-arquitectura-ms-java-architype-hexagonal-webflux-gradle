package co.com.pragma.accounts.usecase;

import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountSearchCriteria;
import co.com.pragma.accounts.ports.IAccountGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class SearchAccountsUseCaseTest {

    @Mock
    private IAccountGateway accountGateway;

    @InjectMocks
    private SearchAccountsUseCase useCase;

    @Test
    void shouldDelegateSearchToGatewayWithLimit() {
        AccountSearchCriteria criteria = AccountSearchCriteria.builder().status("ACTIVE").build();
        when(accountGateway.search(criteria, 50))
            .thenReturn(Flux.just(Account.builder().id("acc-1").status("ACTIVE").build()));

        StepVerifier.create(useCase.execute(criteria, 50))
            .expectNextCount(1)
            .verifyComplete();
    }
}
