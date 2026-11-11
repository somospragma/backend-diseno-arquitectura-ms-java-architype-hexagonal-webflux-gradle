package co.com.pragma.accounts.infrastructure.entrypoint;

import co.com.pragma.accounts.infrastructure.helpers.CorrelationWebFilter;
import co.com.pragma.accounts.model.exception.BusinessException;
import co.com.pragma.accounts.model.exception.DomainException;
import co.com.pragma.accounts.model.exception.ErrorCatalog;
import co.com.pragma.accounts.model.exception.TechnicalException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;
import tools.jackson.databind.json.JsonMapper;

/**
 * Manejo de errores centralizado en formato RFC 7807 (Problem Details) con
 * extensiones de negocio (code, metadata) via el mecanismo de extension del RFC.
 *
 * - Implementa WebExceptionHandler + @Order(-2) (API estable de Spring), NO
 *   @RestControllerAdvice (modelo MVC anotado).
 * - Separa errores de NEGOCIO (4xx) de TECNICOS (5xx) por el tipo de DomainException.
 * - Nunca expone internals (stack trace / mensaje tecnico) en 5xx: mensaje generico.
 * - Jackson 3 (tools.jackson.*), default de Spring Boot 4.
 */
@Order(-2)
public class GlobalErrorWebExceptionHandler implements WebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);
    private static final String TYPE_BASE = "https://errors.pragma.co/";

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        Map<String, Object> problem = toProblem(ex, exchange);
        int status = (int) problem.get("status");
        exchange.getResponse().setStatusCode(HttpStatus.valueOf(status));
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_PROBLEM_JSON);

        byte[] bytes = jsonMapper.writeValueAsBytes(problem);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private Map<String, Object> toProblem(Throwable error, ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().value();
        String traceId = exchange.getAttributeOrDefault(CorrelationWebFilter.TRACE_ID, "");

        if (error instanceof BusinessException be) {
            HttpStatus status = statusForBusinessCode(be.code());
            return problem(status, "Business rule violation", be.getMessage(), path,
                be.code(), be.metadata(), traceId);
        }
        if (error instanceof WebExchangeBindException bind) {
            Map<String, Object> meta = new LinkedHashMap<>();
            bind.getFieldErrors().forEach(fe -> meta.put(fe.getField(), fe.getDefaultMessage()));
            return problem(HttpStatus.BAD_REQUEST, "Validation error",
                ErrorCatalog.VALIDATION_ERROR.defaultMessage(), path,
                ErrorCatalog.VALIDATION_ERROR.code(), meta, traceId);
        }
        if (error instanceof IllegalArgumentException iae) {
            return problem(HttpStatus.BAD_REQUEST, "Invalid request", iae.getMessage(), path,
                ErrorCatalog.VALIDATION_ERROR.code(), Map.of(), traceId);
        }
        if (error instanceof ResponseStatusException rse) {
            HttpStatus status = HttpStatus.valueOf(rse.getStatusCode().value());
            return problem(status, status.getReasonPhrase(), rse.getReason(), path,
                "ACC-" + status.value() + "-000", Map.of(), traceId);
        }

        // Errores tecnicos y no controlados: NUNCA exponer internals al cliente.
        if (error instanceof TechnicalException te) {
            log.error("Technical error [{}] traceId={}", te.code(), traceId, te);
            return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
                ErrorCatalog.UNEXPECTED_ERROR.defaultMessage(), path, te.code(), Map.of(), traceId);
        }
        log.error("Unhandled error traceId={}", traceId, error);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error",
            ErrorCatalog.UNEXPECTED_ERROR.defaultMessage(), path,
            ErrorCatalog.UNEXPECTED_ERROR.code(), Map.of(), traceId);
    }

    private HttpStatus statusForBusinessCode(String code) {
        // El code de negocio embebe el status HTTP: ACC-<status>-<seq>.
        try {
            String[] parts = code.split("-");
            return HttpStatus.valueOf(Integer.parseInt(parts[1]));
        } catch (RuntimeException e) {
            return HttpStatus.UNPROCESSABLE_ENTITY;
        }
    }

    private Map<String, Object> problem(HttpStatus status, String title, String detail, String path,
                                        String code, Map<String, Object> metadata, String traceId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", TYPE_BASE + code);
        body.put("title", title);
        body.put("status", status.value());
        body.put("detail", detail == null ? title : detail);
        body.put("instance", path);
        // Extensiones RFC 7807 (mecanismo de extension del propio RFC):
        body.put("code", code);
        if (!traceId.isBlank()) {
            body.put("traceId", traceId);
        }
        if (metadata != null && !metadata.isEmpty()) {
            body.put("metadata", metadata);
        }
        return body;
    }
}
