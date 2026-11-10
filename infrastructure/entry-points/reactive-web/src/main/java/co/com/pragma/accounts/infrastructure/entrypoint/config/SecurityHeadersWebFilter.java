package co.com.pragma.accounts.infrastructure.entrypoint.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Configura los headers de seguridad en TODAS las respuestas (API-O-006).
 * Mitiga XSS, clickjacking y MIME sniffing. Alineado con OWASP Secure Headers.
 */
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class SecurityHeadersWebFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        HttpHeaders headers = exchange.getResponse().getHeaders();
        headers.set("Content-Security-Policy", "default-src 'self'; frame-ancestors 'none'");
        headers.set("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("X-Frame-Options", "DENY");
        headers.set("Referrer-Policy", "strict-origin-when-cross-origin");
        headers.set("Cache-Control", "no-store");
        return chain.filter(exchange);
    }
}
