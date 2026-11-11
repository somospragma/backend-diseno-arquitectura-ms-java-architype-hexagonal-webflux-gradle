package co.com.pragma.accounts.infrastructure.entrypoint;

import jakarta.validation.constraints.Email;
import java.math.BigDecimal;

/**
 * DTO de actualizacion parcial (HTTP PATCH). Todos los campos son opcionales;
 * solo se aplican los presentes. El email, si viene, debe ser valido.
 */
public record AccountPatchRequest(
    String holderName,
    @Email String email,
    String accountType,
    String status,
    BigDecimal balance
) {}
