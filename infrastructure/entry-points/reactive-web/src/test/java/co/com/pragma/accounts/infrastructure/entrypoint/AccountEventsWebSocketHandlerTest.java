package co.com.pragma.accounts.infrastructure.entrypoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.reactivestreams.Publisher;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AccountEventsWebSocketHandlerTest {

    @Test
    void heartbeatPayloadIsWellFormed() {
        AccountEventsWebSocketHandler handler = new AccountEventsWebSocketHandler();
        assertThat(handler.heartbeatPayload(3)).isEqualTo("{\"type\":\"HEARTBEAT\",\"seq\":3}");
    }

    @Test
    void handleSendsMessagesThroughSession() {
        // Intervalo corto para no esperar en el test.
        AccountEventsWebSocketHandler handler =
            new AccountEventsWebSocketHandler(Duration.ofMillis(5));

        WebSocketSession session = mock(WebSocketSession.class);
        WebSocketMessage msg = mock(WebSocketMessage.class);
        when(session.textMessage(any())).thenReturn(msg);
        // send() consume el Flux de salida; tomamos los 2 primeros y completamos.
        when(session.send(any())).thenAnswer(inv -> {
            Publisher<WebSocketMessage> out = inv.getArgument(0);
            return Flux.from(out).take(2).then();
        });

        StepVerifier.create(handler.handle(session)).verifyComplete();
    }

    @Test
    void handleCompletesWhenSessionSendCompletes() {
        AccountEventsWebSocketHandler handler =
            new AccountEventsWebSocketHandler(Duration.ofMillis(1));
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.textMessage(any())).thenReturn(mock(WebSocketMessage.class));
        when(session.send(any())).thenReturn(Mono.empty());

        StepVerifier.create(handler.handle(session)).verifyComplete();
    }
}
