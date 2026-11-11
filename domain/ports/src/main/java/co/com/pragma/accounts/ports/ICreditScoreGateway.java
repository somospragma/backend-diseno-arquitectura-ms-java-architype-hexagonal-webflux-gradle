package co.com.pragma.accounts.ports;

import co.com.pragma.accounts.model.CreditScore;
import reactor.core.publisher.Mono;

/**
 * Puerto de salida hacia un servicio externo de score crediticio.
 * El nombre es de dominio (CreditScore), nunca el del sistema externo.
 */
public interface ICreditScoreGateway {
    Mono<CreditScore> fetchScore(String accountId);
}
