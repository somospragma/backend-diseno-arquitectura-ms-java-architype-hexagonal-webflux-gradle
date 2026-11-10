package co.com.pragma.accounts.infrastructure.entrypoint;

import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Entry-point WebSocket reactivo (streaming push hacia el cliente). Ejemplifica
 * un canal de eventos de cuenta en tiempo real sobre /ws/accounts/events.
 *
 * El contrato del canal (mensajes, esquema) esta documentado con AsyncAPI en
 * infrastructure/entry-points/reactive-web/src/main/resources/asyncapi.yaml.
 *
 * En un caso real, el flujo de salida provendria de un Sink alimentado por el
 * dominio (p.ej. eventos publicados tras cambios de cuenta). Aqui se emite un
 * heartbeat periodico como demostracion, sin bloquear el event loop.
 */
@Component
public class AccountEventsWebSocketHandler implements WebSocketHandler {

    /** Intervalo del heartbeat de ejemplo. */
    private final Duration interval;

    public AccountEventsWebSocketHandler() {
        this(Duration.ofSeconds(15));
    }

    AccountEventsWebSocketHandler(Duration interval) {
        this.interval = interval;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        Flux<String> outbound = Flux.interval(interval)
            .map(seq -> "{\"type\":\"HEARTBEAT\",\"seq\":" + seq + "}");
        return session.send(outbound.map(session::textMessage));
    }

    /** Visible para test: construye el payload del mensaje N del canal. */
    String heartbeatPayload(long seq) {
        return "{\"type\":\"HEARTBEAT\",\"seq\":" + seq + "}";
    }
}
