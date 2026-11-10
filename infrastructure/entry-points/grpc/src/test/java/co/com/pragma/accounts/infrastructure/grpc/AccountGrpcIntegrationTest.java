package co.com.pragma.accounts.infrastructure.grpc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.grpc.v1.ReactorAccountServiceGrpc;
import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.usecase.GetAccountUseCase;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Test de integracion gRPC de punta a punta usando un canal in-process (sin red
 * ni Docker): levanta el server con el use case mockeado y lo invoca a traves
 * del client adapter. Cubre unary (getAccount) y server-streaming (listAccounts).
 */
class AccountGrpcIntegrationTest {

    private final GetAccountUseCase getAccountUseCase = mock(GetAccountUseCase.class);
    private Server server;
    private ManagedChannel channel;
    private AccountGrpcClientAdapter client;

    @BeforeEach
    void setUp() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor()
            .addService(new AccountGrpcService(getAccountUseCase))
            .build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        client = new AccountGrpcClientAdapter(ReactorAccountServiceGrpc.newReactorStub(channel));
    }

    @AfterEach
    void tearDown() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void unaryGetAccountShouldRoundTrip() {
        when(getAccountUseCase.execute("acc-1")).thenReturn(Mono.just(
            Account.builder().id("acc-1").holderName("Ada").accountType("SAVINGS").status("ACTIVE").build()));

        StepVerifier.create(client.getAccount("acc-1"))
            .assertNext(a -> {
                org.junit.jupiter.api.Assertions.assertEquals("acc-1", a.getId());
                org.junit.jupiter.api.Assertions.assertEquals("Ada", a.getHolderName());
            })
            .verifyComplete();
    }

    @Test
    void serverStreamingListAccountsShouldRoundTrip() {
        when(getAccountUseCase.findAll(anyInt())).thenReturn(Flux.just(
            Account.builder().id("acc-1").status("ACTIVE").build(),
            Account.builder().id("acc-2").status("ACTIVE").build()));

        StepVerifier.create(client.listAccounts(10))
            .expectNextCount(2)
            .verifyComplete();
    }
}
