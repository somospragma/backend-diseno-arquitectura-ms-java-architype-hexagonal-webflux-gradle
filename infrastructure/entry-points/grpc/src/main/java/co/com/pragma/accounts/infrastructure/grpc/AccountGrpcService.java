package co.com.pragma.accounts.infrastructure.grpc;

import co.com.pragma.accounts.grpc.v1.AccountReply;
import co.com.pragma.accounts.grpc.v1.GetAccountRequest;
import co.com.pragma.accounts.grpc.v1.ListAccountsRequest;
import co.com.pragma.accounts.grpc.v1.ReactorAccountServiceGrpc;
import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.usecase.GetAccountUseCase;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Entry point gRPC (server) reactivo (MSG-BP-006). Implementa el stub generado por
 * Reactor gRPC y delega en los use cases del dominio; no contiene logica de negocio.
 * Ejemplifica RPC unary (GetAccount) y server-streaming (ListAccounts con tope).
 */
@RequiredArgsConstructor
public class AccountGrpcService extends ReactorAccountServiceGrpc.AccountServiceImplBase {

    private static final int MAX_LIMIT = 100;

    private final GetAccountUseCase getAccountUseCase;

    @Override
    public Mono<AccountReply> getAccount(Mono<GetAccountRequest> request) {
        return request
            .map(GetAccountRequest::getId)
            .flatMap(getAccountUseCase::execute)
            .map(this::toReply);
    }

    @Override
    public Flux<AccountReply> listAccounts(Mono<ListAccountsRequest> request) {
        return request
            .map(req -> Math.min(Math.max(req.getLimit(), 1), MAX_LIMIT)) // tope duro
            .flatMapMany(getAccountUseCase::findAll)
            .map(this::toReply);
    }

    private AccountReply toReply(Account account) {
        return AccountReply.newBuilder()
            .setId(nullToEmpty(account.getId()))
            .setHolderName(nullToEmpty(account.getHolderName()))
            .setAccountType(nullToEmpty(account.getAccountType()))
            .setStatus(nullToEmpty(account.getStatus()))
            .build();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
