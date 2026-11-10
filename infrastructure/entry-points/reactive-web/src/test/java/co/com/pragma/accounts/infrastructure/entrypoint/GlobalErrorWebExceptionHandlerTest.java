package co.com.pragma.accounts.infrastructure.entrypoint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import co.com.pragma.accounts.model.AccountNotFoundException;
import co.com.pragma.accounts.model.exception.ErrorCatalog;
import co.com.pragma.accounts.model.exception.TechnicalException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GlobalErrorWebExceptionHandlerTest {

    private final GlobalErrorWebExceptionHandler handler = new GlobalErrorWebExceptionHandler();

    private ServerWebExchange exchange() {
        return MockServerWebExchange.from(
            MockServerHttpRequest.get("/api/v1/accounts/x").build());
    }

    private String bodyOf(ServerWebExchange ex) {
        var buffers = ((org.springframework.mock.http.server.reactive.MockServerHttpResponse) ex.getResponse());
        return buffers.getBodyAsString().block();
    }

    @Test
    void businessErrorMaps4xxWithCode() {
        ServerWebExchange ex = exchange();
        StepVerifier.create(handler.handle(ex, new AccountNotFoundException("x"))).verifyComplete();
        assertEquals(HttpStatus.NOT_FOUND, ex.getResponse().getStatusCode());
        assertEquals(MediaType.APPLICATION_PROBLEM_JSON, ex.getResponse().getHeaders().getContentType());
        assertTrue(bodyOf(ex).contains("ACC-404-001"));
    }

    @Test
    void illegalArgumentMaps400() {
        ServerWebExchange ex = exchange();
        StepVerifier.create(handler.handle(ex, new IllegalArgumentException("bad"))).verifyComplete();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getResponse().getStatusCode());
    }

    @Test
    void responseStatusExceptionMapsItsStatus() {
        ServerWebExchange ex = exchange();
        StepVerifier.create(handler.handle(ex, new ResponseStatusException(HttpStatus.CONFLICT, "dup")))
            .verifyComplete();
        assertEquals(HttpStatus.CONFLICT, ex.getResponse().getStatusCode());
    }

    @Test
    void technicalErrorMaps500WithoutLeakingInternals() {
        ServerWebExchange ex = exchange();
        StepVerifier.create(handler.handle(ex,
                new TechnicalException(ErrorCatalog.UNEXPECTED_ERROR.code(), "db down at host:5432", new RuntimeException())))
            .verifyComplete();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getResponse().getStatusCode());
        String body = bodyOf(ex);
        assertTrue(body.contains("Unexpected error"));
        assertTrue(!body.contains("db down at host"));
    }

    @Test
    void unhandledErrorMaps500() {
        ServerWebExchange ex = exchange();
        StepVerifier.create(handler.handle(ex, new RuntimeException("boom"))).verifyComplete();
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getResponse().getStatusCode());
    }
}
