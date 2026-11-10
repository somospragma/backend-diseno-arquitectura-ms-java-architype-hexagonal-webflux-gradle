package co.com.pragma.accounts.infrastructure.entrypoint.config;

import co.com.pragma.accounts.infrastructure.entrypoint.GlobalErrorWebExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Beans del entry-point reactivo que no llevan @Component (para mantener las
 * clases de dominio del handler libres de anotaciones de escaneo).
 */
@Configuration
public class WebEntryPointConfig {

    @Bean
    public GlobalErrorWebExceptionHandler globalErrorWebExceptionHandler() {
        return new GlobalErrorWebExceptionHandler();
    }

    @Bean
    public SecurityHeadersWebFilter securityHeadersWebFilter() {
        return new SecurityHeadersWebFilter();
    }
}
