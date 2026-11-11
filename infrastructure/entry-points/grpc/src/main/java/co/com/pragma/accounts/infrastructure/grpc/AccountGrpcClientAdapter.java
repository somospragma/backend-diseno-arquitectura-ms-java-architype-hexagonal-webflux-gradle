package co.com.pragma.accounts.infrastructure.grpc;

import co.com.pragma.accounts.grpc.v1.AccountReply;
import co.com.pragma.accounts.grpc.v1.GetAccountRequest;
import co.com.pragma.accounts.grpc.v1.ListAccountsRequest;
import co.com.pragma.accounts.grpc.v1.ReactorAccountServiceGrpc;
import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.exception.ErrorCatalog;
import co.com.pragma.accounts.model.exception.TechnicalException;
import co.com.pragma.accounts.ports.IExternalAccountGateway;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Driven-adapter cliente gRPC hacia otro microservicio de cuentas. Usa el stub
 * reactivo generado por Reactor gRPC y mapea la respuesta al modelo de dominio.
 * Traduce fallos de transporte a TechnicalException (no propaga el error crudo).
 */
@RequiredArgsConstructor
public class AccountGrpcClientAdapter implements IExternalAccountGateway {

    private final ReactorAccountServiceGrpc.ReactorAccountServiceStub stub;

    @Override
    public Mono<Account> getAccount(String id) {
        return Mono.just(GetAccountRequest.newBuilder().setId(id).build())
            .transform(stub::getAccount)
            .map(this::toModel)
            .onErrorMap(this::notDomainError, e -> new TechnicalException(
                ErrorCatalog.UNEXPECTED_ERROR.code(), "gRPC getAccount failed", e));
    }

    @Override
    public Flux<Account> listAccounts(int limit) {
        return Mono.just(ListAccountsRequest.newBuilder().setLimit(limit).build())
            .as(stub::listAccounts)
            .map(this::toModel)
            .onErrorMap(this::notDomainError, e -> new TechnicalException(
                ErrorCatalog.UNEXPECTED_ERROR.code(), "gRPC listAccounts failed", e));
    }

    private boolean notDomainError(Throwable t) {
        return !(t instanceof co.com.pragma.accounts.model.exception.DomainException);
    }

    private Account toModel(AccountReply reply) {
        return Account.builder()
            .id(reply.getId())
            .holderName(reply.getHolderName())
            .accountType(reply.getAccountType())
            .status(reply.getStatus())
            .build();
    }
}
