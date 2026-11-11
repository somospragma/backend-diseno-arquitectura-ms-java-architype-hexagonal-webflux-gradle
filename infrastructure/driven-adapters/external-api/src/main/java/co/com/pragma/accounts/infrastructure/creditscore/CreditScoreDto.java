package co.com.pragma.accounts.infrastructure.creditscore;

/** DTO de transporte del servicio externo. No sale del adapter. */
public record CreditScoreDto(String accountId, int score, String band) {
}
