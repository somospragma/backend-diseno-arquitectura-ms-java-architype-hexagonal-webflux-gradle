package co.com.pragma.accounts.application.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

/**
 * Habilita la propagacion automatica de contexto de Reactor (OBS-O-002): el
 * traceId escrito en Reactor Context por el CorrelationWebFilter se puentea a
 * MDC para que aparezca en los logs aunque haya saltos de scheduler.
 */
@Configuration
public class ReactorContextConfig {

    @PostConstruct
    public void enableContextPropagation() {
        Hooks.enableAutomaticContextPropagation();
    }
}
