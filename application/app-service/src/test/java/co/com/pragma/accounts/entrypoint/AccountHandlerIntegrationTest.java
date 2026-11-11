package co.com.pragma.accounts.entrypoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import co.com.pragma.accounts.application.MainApplication;
import co.com.pragma.accounts.model.Account;
import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.usecase.CreateAccountUseCase;
import co.com.pragma.accounts.usecase.GetAccountUseCase;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@SpringBootTest(
    classes = MainApplication.class,
    webEnvironment = WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.r2dbc.url=r2dbc:h2:mem:///testdb;DB_CLOSE_DELAY=-1",
        "spring.r2dbc.username=sa",
        "spring.r2dbc.password="
    }
)
class AccountHandlerIntegrationTest {

    @LocalServerPort
    private int port;

    @MockitoBean
    private GetAccountUseCase getAccountUseCase;

    @MockitoBean
    private CreateAccountUseCase createAccountUseCase;

    @MockitoBean
    private co.com.pragma.accounts.usecase.DeleteAccountUseCase deleteAccountUseCase;

    @MockitoBean
    private co.com.pragma.accounts.usecase.UpdateAccountUseCase updateAccountUseCase;

    @MockitoBean
    private co.com.pragma.accounts.usecase.PatchAccountUseCase patchAccountUseCase;

    @MockitoBean
    private co.com.pragma.accounts.usecase.SearchAccountsUseCase searchAccountsUseCase;

    private WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void shouldReturnAccountByIdWithMaskedEmail() {
        Account account = Account.builder()
            .id("acc-1").holderName("Ada Lovelace").email("ada.lovelace@pragma.co")
            .accountType("SAVINGS").balance(BigDecimal.TEN).status("ACTIVE").build();
        when(getAccountUseCase.execute("acc-1")).thenReturn(Mono.just(account));

        client.get().uri("/api/v1/accounts/acc-1")
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.id").isEqualTo("acc-1")
            .jsonPath("$.status").isEqualTo("ACTIVE")
            // El email (PII) sale enmascarado, nunca completo (DSC-O-003).
            .jsonPath("$.email").isEqualTo("****@pragma.co");
    }

    @Test
    void shouldReturnRfc7807ProblemWithCodeAndMetadataOnBusinessError() {
        when(getAccountUseCase.execute("missing")).thenReturn(Mono.error(new AccountNotFoundException("missing")));

        client.get().uri("/api/v1/accounts/missing")
            .exchange()
            .expectStatus().isNotFound()
            .expectHeader().contentType("application/problem+json")
            .expectBody()
            .jsonPath("$.type").exists()
            .jsonPath("$.title").exists()
            .jsonPath("$.status").isEqualTo(404)
            .jsonPath("$.code").isEqualTo("ACC-404-001")
            .jsonPath("$.metadata.accountId").isEqualTo("missing");
    }

    @Test
    void shouldReturn400ProblemOnInvalidInput() {
        // holderName en blanco viola @NotBlank -> validacion 400 RFC7807
        client.post().uri("/api/v1/accounts")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("holderName", "", "accountType", "SAVINGS", "initialBalance", 10))
            .exchange()
            .expectStatus().isBadRequest()
            .expectHeader().contentType("application/problem+json")
            .expectBody()
            .jsonPath("$.code").isEqualTo("ACC-400-001")
            .jsonPath("$.metadata.holderName").exists();
    }

    @Test
    void shouldNotLeakInternalsOnTechnicalError() {
        when(getAccountUseCase.execute("boom"))
            .thenReturn(Mono.error(new RuntimeException("connection reset by peer at internal.host")));

        client.get().uri("/api/v1/accounts/boom")
            .exchange()
            .expectStatus().is5xxServerError()
            .expectBody()
            .jsonPath("$.status").isEqualTo(500)
            .jsonPath("$.detail").isEqualTo("Unexpected error")
            .jsonPath("$.code").isEqualTo("ACC-500-001");
    }

    @Test
    void shouldSetSecurityHeaders() {
        Account account = Account.builder().id("acc-1").status("ACTIVE").build();
        when(getAccountUseCase.execute("acc-1")).thenReturn(Mono.just(account));

        client.get().uri("/api/v1/accounts/acc-1")
            .exchange()
            .expectStatus().isOk()
            .expectHeader().valueEquals("X-Content-Type-Options", "nosniff")
            .expectHeader().valueEquals("X-Frame-Options", "DENY")
            .expectHeader().exists("Content-Security-Policy")
            .expectHeader().exists("Strict-Transport-Security")
            .expectHeader().exists("Referrer-Policy")
            .expectHeader().exists("X-Request-ID");
    }

    @Test
    void shouldExposeLivenessHealth() {
        client.get().uri("/actuator/health/liveness")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void shouldCreateAccountReturning201() {
        Account created = Account.builder()
            .id("new-1").holderName("Grace Hopper").email("grace@pragma.co")
            .accountType("CHECKING").balance(BigDecimal.valueOf(100)).status("ACTIVE").build();
        when(createAccountUseCase.execute(any(Account.class))).thenReturn(Mono.just(created));

        client.post().uri("/api/v1/accounts")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "holderName", "Grace Hopper",
                "email", "grace@pragma.co",
                "accountType", "CHECKING",
                "initialBalance", 100))
            .exchange()
            .expectStatus().isCreated()
            .expectBody()
            .jsonPath("$.id").isEqualTo("new-1")
            .jsonPath("$.email").isEqualTo("****@pragma.co");
    }

    @Test
    void shouldListAccountsWithPaginationCap() {
        Account a = Account.builder().id("acc-1").status("ACTIVE").build();
        when(getAccountUseCase.findAll(org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(reactor.core.publisher.Flux.just(a));

        client.get().uri("/api/v1/accounts?size=9999")
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$[0].id").isEqualTo("acc-1");
    }

    @Test
    void shouldDeleteAccountReturning204() {
        when(deleteAccountUseCase.execute("acc-1")).thenReturn(Mono.empty());

        client.delete().uri("/api/v1/accounts/acc-1")
            .exchange()
            .expectStatus().isNoContent();
    }

    @Test
    void shouldReplaceAccountWithPut() {
        Account updated = Account.builder()
            .id("acc-1").holderName("Ada L.").email("ada@pragma.co")
            .accountType("SAVINGS").balance(BigDecimal.ONE).status("ACTIVE").build();
        when(updateAccountUseCase.execute(org.mockito.ArgumentMatchers.eq("acc-1"), any(Account.class)))
            .thenReturn(Mono.just(updated));

        client.put().uri("/api/v1/accounts/acc-1")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(Map.of(
                "holderName", "Ada L.",
                "email", "ada@pragma.co",
                "accountType", "SAVINGS",
                "initialBalance", 1))
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.holderName").isEqualTo("Ada L.");
    }

    @Test
    void shouldPartiallyUpdateAccountWithPatch() {
        Account patched = Account.builder()
            .id("acc-1").holderName("Ada").email("ada@pragma.co")
            .accountType("SAVINGS").balance(BigDecimal.TEN).status("FROZEN").build();
        when(patchAccountUseCase.execute(org.mockito.ArgumentMatchers.eq("acc-1"), any(Account.class)))
            .thenReturn(Mono.just(patched));

        client.patch().uri("/api/v1/accounts/acc-1")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("status", "FROZEN"))
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$.status").isEqualTo("FROZEN");
    }

    @Test
    void shouldSearchAccountsWithQueryMethod() {
        Account a = Account.builder().id("acc-1").status("ACTIVE").accountType("SAVINGS").build();
        when(searchAccountsUseCase.execute(any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(reactor.core.publisher.Flux.just(a));

        client.method(org.springframework.http.HttpMethod.valueOf("QUERY"))
            .uri("/api/v1/accounts/search")
            .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("status", "ACTIVE", "accountType", "SAVINGS"))
            .exchange()
            .expectStatus().isOk()
            .expectBody().jsonPath("$[0].id").isEqualTo("acc-1");
    }
}
