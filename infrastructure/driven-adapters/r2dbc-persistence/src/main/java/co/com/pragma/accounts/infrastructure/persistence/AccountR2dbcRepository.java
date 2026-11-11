package co.com.pragma.accounts.infrastructure.persistence;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface AccountR2dbcRepository extends ReactiveCrudRepository<AccountEntity, String> {

    Flux<AccountEntity> findByStatus(String status);

    /** Consulta con LIMIT aplicado en SQL (DAT-O-005 / DAT-O-007). */
    @Query("SELECT * FROM accounts LIMIT :limit")
    Flux<AccountEntity> findAllWithLimit(int limit);

    /**
     * Busqueda por criterios (metodo HTTP QUERY). Los filtros nulos no aplican
     * (busqueda por ejemplo) via el patron ':param IS NULL OR col = :param'.
     * Query parametrizada: nunca concatenacion (DAT-O-002). LIMIT en SQL.
     */
    @Query("""
        SELECT * FROM accounts
        WHERE (:status IS NULL OR status = :status)
          AND (:accountType IS NULL OR account_type = :accountType)
          AND (:holderName IS NULL OR holder_name = :holderName)
        LIMIT :limit
        """)
    Flux<AccountEntity> search(String status, String accountType, String holderName, int limit);
}
