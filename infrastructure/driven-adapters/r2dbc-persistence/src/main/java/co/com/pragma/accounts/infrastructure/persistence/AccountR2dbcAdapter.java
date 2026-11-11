package co.com.pragma.accounts.infrastructure.persistence;

import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.ports.IAccountGateway;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class AccountR2dbcAdapter implements IAccountGateway {
    private final AccountR2dbcRepository r2dbcRepository;
    private final AccountEntityMapper mapper;

    @Override
    public Mono<Account> save(Account account) {
        return r2dbcRepository.save(mapper.toEntity(account))
            .map(mapper::toModel);
    }

    @Override
    public Mono<Account> findById(String id) {
        return r2dbcRepository.findById(id)
            .map(mapper::toModel);
    }

    @Override
    public Flux<Account> findAll(int limit) {
        // Paginacion con tope real en la consulta (DAT-O-005): nunca traer la
        // tabla completa a memoria. El limite se aplica en SQL, no en el pipeline.
        return r2dbcRepository.findAllWithLimit(limit)
            .map(mapper::toModel);
    }

    @Override
    public Mono<Void> deleteById(String id) {
        return r2dbcRepository.deleteById(id);
    }

    @Override
    public Mono<Boolean> existsById(String id) {
        return r2dbcRepository.existsById(id);
    }

    @Override
    public Flux<Account> search(co.com.pragma.accounts.model.AccountSearchCriteria criteria, int limit) {
        return r2dbcRepository.search(
                criteria.status(), criteria.accountType(), criteria.holderName(), limit)
            .map(mapper::toModel);
    }
}
