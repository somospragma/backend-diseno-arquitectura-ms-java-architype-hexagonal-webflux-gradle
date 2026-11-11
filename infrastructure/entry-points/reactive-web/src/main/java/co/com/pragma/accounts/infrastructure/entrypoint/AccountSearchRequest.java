package co.com.pragma.accounts.infrastructure.entrypoint;

/**
 * DTO de criterios de busqueda para el metodo HTTP QUERY. Todos los campos son
 * opcionales; los nulos no filtran (busqueda por ejemplo).
 */
public record AccountSearchRequest(String status, String accountType, String holderName) {}
