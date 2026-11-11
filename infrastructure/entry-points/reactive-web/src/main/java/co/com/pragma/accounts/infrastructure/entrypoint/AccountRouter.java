package co.com.pragma.accounts.infrastructure.entrypoint;

import static org.springframework.web.reactive.function.server.RequestPredicates.method;
import static org.springframework.web.reactive.function.server.RequestPredicates.path;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

/**
 * Enrutamiento reactivo que ejemplifica TODOS los metodos HTTP sobre el recurso
 * accounts: GET, POST, PUT, PATCH, DELETE y el metodo QUERY (busqueda con body,
 * IETF HTTP QUERY). QUERY no tiene atajo en RouterFunctions, por eso se declara
 * con method(HttpMethod.valueOf("QUERY")).
 */
@Configuration
@RequiredArgsConstructor
public class AccountRouter {
    private final AccountHandler handler;

    @Bean
    public RouterFunction<ServerResponse> accountRoutes() {
        return RouterFunctions.route()
            .path("/api/v1/accounts", builder -> builder
                .GET("", handler::findAll)
                .GET("/{id}", handler::findById)
                .POST("", handler::create)
                .PUT("/{id}", handler::update)
                .PATCH("/{id}", handler::patch)
                .DELETE("/{id}", handler::delete)
            )
            // HTTP QUERY: busqueda con body sobre la coleccion.
            .route(method(HttpMethod.valueOf("QUERY")).and(path("/api/v1/accounts/search")),
                handler::search)
            .build();
    }
}
