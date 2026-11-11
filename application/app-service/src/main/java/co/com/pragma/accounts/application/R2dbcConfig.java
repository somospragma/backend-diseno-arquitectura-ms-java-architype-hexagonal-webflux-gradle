package co.com.pragma.accounts.application;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

/**
 * Habilita el escaneo de repositorios R2DBC en el paquete de persistencia.
 * Necesario porque MainApplication vive en ...application y los repositorios
 * en ...infrastructure.persistence (fuera del paquete base por defecto).
 */
@Configuration
@EnableR2dbcRepositories(basePackages = "co.com.pragma.accounts.infrastructure.persistence")
public class R2dbcConfig {
}
