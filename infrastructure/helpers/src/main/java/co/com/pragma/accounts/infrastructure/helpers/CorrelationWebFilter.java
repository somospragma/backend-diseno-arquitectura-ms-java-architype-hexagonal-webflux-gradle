package co.com.pragma.accounts.infrastructure.helpers;

import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * Correlacion de peticiones (OBS-O-002). Toma o genera el traceId y lo:
 *  - devuelve en el header de respuesta,
 *  - guarda como atributo del exchange (lectura sincrona, p.ej. el error handler),
 *  - propaga por Reactor Context (nunca ThreadLocal/MDC crudo, que no sobreviven
 *    los saltos de scheduler). El puente a MDC para los logs lo hace la
 *    propagacion de contexto de Micrometer (Hooks.enableAutomaticContextPropagation()).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationWebFilter implements WebFilter {

    public static final String TRACE_ID = "traceId";
    public static final String HEADER = "X-Request-ID";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String incoming = exchange.getRequest().getHeaders().getFirst(HEADER);
        String traceId = (incoming == null || incoming.isBlank())
            ? UUID.randomUUID().toString()
            : incoming;

        exchange.getResponse().getHeaders().add(HEADER, traceId);
        exchange.getAttributes().put(TRACE_ID, traceId);

        return chain.filter(exchange)
            .contextWrite(Context.of(TRACE_ID, traceId))
            .doOnEach(signal -> {
                // Puente puntual a MDC para que los logs sincronos lleven el traceId.
                if (signal.isOnComplete() || signal.isOnError()) {
                    MDC.remove(TRACE_ID);
                }
            })
            .doFirst(() -> MDC.put(TRACE_ID, traceId));
    }
}
