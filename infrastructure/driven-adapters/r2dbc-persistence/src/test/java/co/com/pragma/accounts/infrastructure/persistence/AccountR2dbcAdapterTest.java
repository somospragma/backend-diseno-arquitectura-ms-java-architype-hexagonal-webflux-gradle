package co.com.pragma.accounts.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.model.Account;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AccountR2dbcAdapterTest {

    private final AccountR2dbcRepository repo = Mockito.mock(AccountR2dbcRepository.class);
    private final AccountEntityMapper mapper = Mappers.getMapper(AccountEntityMapper.class);
    private final AccountR2dbcAdapter adapter = new AccountR2dbcAdapter(repo, mapper);

    private AccountEntity entity(String id) {
        return AccountEntity.builder().id(id).holderName("Ada").email("a@pragma.co")
            .accountType("SAVINGS").status("ACTIVE").build();
    }

    @Test
    void shouldSave() {
        when(repo.save(any(AccountEntity.class))).thenReturn(Mono.just(entity("acc-1")));
        StepVerifier.create(adapter.save(Account.builder().id("acc-1").build()))
            .assertNext(a -> assertEquals("acc-1", a.getId()))
            .verifyComplete();
    }

    @Test
    void shouldFindById() {
        when(repo.findById("acc-1")).thenReturn(Mono.just(entity("acc-1")));
        StepVerifier.create(adapter.findById("acc-1"))
            .assertNext(a -> assertEquals("Ada", a.getHolderName()))
            .verifyComplete();
    }

    @Test
    void shouldFindAllWithLimit() {
        when(repo.findAllWithLimit(anyInt())).thenReturn(Flux.just(entity("acc-1"), entity("acc-2")));
        StepVerifier.create(adapter.findAll(10))
            .expectNextCount(2)
            .verifyComplete();
    }

    @Test
    void shouldDeleteById() {
        when(repo.deleteById(anyString())).thenReturn(Mono.empty());
        StepVerifier.create(adapter.deleteById("acc-1")).verifyComplete();
    }

    @Test
    void shouldCheckExistsById() {
        when(repo.existsById("acc-1")).thenReturn(Mono.just(true));
        StepVerifier.create(adapter.existsById("acc-1")).expectNext(true).verifyComplete();
    }

    @Test
    void shouldSearchByCriteria() {
        when(repo.search(anyString(), anyString(), any(), anyInt()))
            .thenReturn(Flux.just(entity("acc-1")));
        var criteria = co.com.pragma.accounts.model.AccountSearchCriteria.builder()
            .status("ACTIVE").accountType("SAVINGS").build();
        StepVerifier.create(adapter.search(criteria, 10))
            .assertNext(a -> assertEquals("acc-1", a.getId()))
            .verifyComplete();
    }
}
