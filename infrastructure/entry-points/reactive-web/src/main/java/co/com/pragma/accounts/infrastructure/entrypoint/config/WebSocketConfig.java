package co.com.pragma.accounts.infrastructure.entrypoint.config;

import co.com.pragma.accounts.infrastructure.entrypoint.AccountEventsWebSocketHandler;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;

/**
 * Wiring del entry-point WebSocket reactivo. Registra el handler del canal de
 * eventos en /ws/accounts/events via SimpleUrlHandlerMapping (WebFlux resuelve
 * el upgrade con reactor-netty; no requiere dependencia extra).
 */
@Configuration
public class WebSocketConfig {

    @Bean
    public HandlerMapping webSocketHandlerMapping(AccountEventsWebSocketHandler handler) {
        Map<String, WebSocketHandler> map = Map.of("/ws/accounts/events", handler);
        SimpleUrlHandlerMapping mapping = new SimpleUrlHandlerMapping();
        mapping.setUrlMap(map);
        // Antes del mapeo de endpoints funcionales/anotados.
        mapping.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return mapping;
    }
}
