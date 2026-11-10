package co.com.pragma.accounts.infrastructure.entrypoint;

import co.com.pragma.accounts.usecase.CreateAccountUseCase;
import co.com.pragma.accounts.usecase.DeleteAccountUseCase;
import co.com.pragma.accounts.usecase.GetAccountUseCase;
import co.com.pragma.accounts.usecase.PatchAccountUseCase;
import co.com.pragma.accounts.usecase.SearchAccountsUseCase;
import co.com.pragma.accounts.usecase.UpdateAccountUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

/**
 * Handler reactivo que ejemplifica TODOS los metodos HTTP (API-O-001):
 * GET (uno/lista), POST (crear, 201), PUT (reemplazo total), PATCH (parcial),
 * DELETE (204) y QUERY (busqueda con body, idempotente y segura). Solo delega
 * en los use cases; no contiene logica de negocio.
 */
@Component
@RequiredArgsConstructor
public class AccountHandler {

    /** Tope duro de tamano de pagina (API-O-005): nunca colecciones ilimitadas. */
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 20;

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final PatchAccountUseCase patchAccountUseCase;
    private final DeleteAccountUseCase deleteAccountUseCase;
    private final SearchAccountsUseCase searchAccountsUseCase;
    private final AccountRestMapper accountRestMapper;
    private final RequestValidator validator;

    public Mono<ServerResponse> findById(ServerRequest request) {
        String id = request.pathVariable("id");
        return getAccountUseCase.execute(id)
            .map(accountRestMapper::toResponse)
            .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> findAll(ServerRequest request) {
        int limit = clampSize(request.queryParam("size").orElse(null));
        return getAccountUseCase.findAll(limit)
            .map(accountRestMapper::toResponse)
            .collectList()
            .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }

    public Mono<ServerResponse> create(ServerRequest request) {
        return request.bodyToMono(AccountRequest.class)
            .map(validator::validate)
            .map(accountRestMapper::toModel)
            .flatMap(createAccountUseCase::execute)
            .map(accountRestMapper::toResponse)
            .flatMap(response -> ServerResponse.status(HttpStatus.CREATED).bodyValue(response));
    }

    /** HTTP PUT: reemplazo total (idempotente). */
    public Mono<ServerResponse> update(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(AccountRequest.class)
            .map(validator::validate)
            .map(accountRestMapper::toModel)
            .flatMap(model -> updateAccountUseCase.execute(id, model))
            .map(accountRestMapper::toResponse)
            .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    /** HTTP PATCH: actualizacion parcial (solo campos presentes). */
    public Mono<ServerResponse> patch(ServerRequest request) {
        String id = request.pathVariable("id");
        return request.bodyToMono(AccountPatchRequest.class)
            .map(validator::validate)
            .map(accountRestMapper::toModel)
            .flatMap(patch -> patchAccountUseCase.execute(id, patch))
            .map(accountRestMapper::toResponse)
            .flatMap(response -> ServerResponse.ok().bodyValue(response));
    }

    public Mono<ServerResponse> delete(ServerRequest request) {
        String id = request.pathVariable("id");
        return deleteAccountUseCase.execute(id)
            .then(ServerResponse.noContent().build());
    }

    /** HTTP QUERY: busqueda con body (segura e idempotente), con tope de resultados. */
    public Mono<ServerResponse> search(ServerRequest request) {
        int limit = clampSize(request.queryParam("size").orElse(null));
        return request.bodyToMono(AccountSearchRequest.class)
            .map(accountRestMapper::toCriteria)
            .flatMapMany(criteria -> searchAccountsUseCase.execute(criteria, limit))
            .map(accountRestMapper::toResponse)
            .collectList()
            .flatMap(list -> ServerResponse.ok().bodyValue(list));
    }

    private int clampSize(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT_PAGE_SIZE;
        }
        try {
            return Math.min(Math.max(Integer.parseInt(raw), 1), MAX_PAGE_SIZE);
        } catch (NumberFormatException e) {
            return DEFAULT_PAGE_SIZE;
        }
    }
}
