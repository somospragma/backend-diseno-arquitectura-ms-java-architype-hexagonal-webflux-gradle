# gRPC reactivo (snippet de referencia)

Comunicación entre microservicios internos con gRPC (MSG-BP-006). Se documenta como snippet — activar solo si el servicio expone/consume gRPC — porque requiere el plugin de protobuf y la generación de stubs, que no se incluye en el build base para mantenerlo liviano.

## 1. Contrato `.proto` versionado (fuente de verdad)

```proto
syntax = "proto3";
package co.com.pragma.accounts.grpc.v1;
option java_multiple_files = true;

service AccountService {
  rpc GetAccount (GetAccountRequest) returns (AccountReply);
}

message GetAccountRequest { string id = 1; }
message AccountReply { string id = 1; string status = 2; }
```

## 2. Dependencias (Gradle, ya en el version catalog)

```kotlin
plugins { id("com.google.protobuf") version "0.9.4" }

dependencies {
    implementation(platform(libs.grpc.bom))
    implementation(libs.grpc.stub)
    implementation(libs.grpc.protobuf)
    implementation(libs.grpc.netty.shaded)
    // Reactor gRPC para exponer stubs como Mono/Flux
    implementation("com.salesforce.servicelibs:reactor-grpc-stub:1.2.4")
}
```

## 3. Handler reactivo (server) con Reactor gRPC

```java
public class AccountGrpcService extends ReactorAccountServiceGrpc.AccountServiceImplBase {
    private final GetAccountUseCase useCase;

    @Override
    public Mono<AccountReply> getAccount(Mono<GetAccountRequest> request) {
        return request
            .flatMap(req -> useCase.execute(req.getId()))   // deadline/timeout del lado cliente
            .map(acc -> AccountReply.newBuilder()
                .setId(acc.getId()).setStatus(acc.getStatus()).build());
    }
}
```

## Reglas (framework de madurez)
- Contrato `.proto` versionado y con compatibilidad hacia atrás.
- Deadlines/timeouts en cada llamada (equivalente al timeout HTTP).
- Status codes gRPC correctos (nunca OK con error adentro).
- Interceptores para observabilidad (correlation id) y TLS/mTLS.
- gRPC reflection deshabilitado en producción.
