package co.com.pragma.accounts.ports;

import java.time.Duration;
import reactor.core.publisher.Mono;

/**
 * Puerto de idempotencia para consumidores at-least-once (MSG-O-001 / RES-O-004).
 * markIfFirst registra la clave (id de mensaje/evento) de forma atomica y
 * devuelve true SOLO la primera vez; en reprocesos devuelve false para saltar el
 * efecto. El TTL acota la ventana de deduplicacion.
 */
public interface IIdempotencyStore {

    Mono<Boolean> markIfFirst(String key, Duration ttl);
}
