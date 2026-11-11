# accounts-service (Artefacto showcase — Java WebFlux / Gradle)

Microservicio reactivo de referencia de Pragma que cumple **punto a punto** el Framework de Madurez Backend (Obligatorios + Best Practice). Un desarrollador puede clonarlo, correrlo y usarlo como base o como muestra del "cómo se hace" cada práctica.

## Información General

- **Framework**: Spring Boot 4.x (WebFlux, reactivo, Netty)
- **Lenguaje**: Java 21
- **Build**: Gradle multi-módulo (Kotlin DSL) con version catalog
- **Arquitectura**: Hexagonal (puertos y adaptadores)
- **Persistencia**: R2DBC (PostgreSQL) + adapter DynamoDB de muestra
- **Paquete base**: `co.com.pragma.accounts`
- **Cobertura**: gate de 90% de línea (JaCoCo) enganchado a `check`

## Comandos

```bash
./gradlew test                          # tests unitarios + integración
./gradlew check                         # tests + gate de cobertura 90% + arch tests
./gradlew jacocoRootReport              # reporte de cobertura unificado (build/reports/jacoco)
./gradlew jacocoRootCoverageVerification# gate de cobertura (falla si < 90%)
./gradlew :domain:usecases:pitest       # mutation testing del dominio
./gradlew :application:app-service:bootRun  # ejecutar el servicio
```

## Estructura

```
accounts-service/
├── domain/
│   ├── model/         # Account, CreditScore, AccountEvent, excepciones, @Sensitive
│   ├── ports/         # IAccountGateway, ICreditScoreGateway, IAccountEventGateway, ICacheGateway, IEventPublisher
│   └── usecases/      # Create/Get/Delete/GetCreditScore UseCase (framework-free)
├── infrastructure/
│   ├── entry-points/reactive-web/     # Router + Handler + RFC7807 handler + security headers + validación
│   ├── driven-adapters/
│   │   ├── r2dbc-persistence/         # Postgres reactivo, paginación con tope
│   │   ├── external-api/              # WebClient resiliente (timeout+retry+CB+bulkhead)
│   │   ├── dynamodb-persistence/      # DynamoDB (Query nunca Scan)  [dynamodb.enabled]
│   │   ├── sqs-consumer/              # Consumidor con aislamiento por mensaje + DLQ
│   │   ├── redis-cache/               # Cache reactivo con TTL + degradación  [redis.enabled]
│   │   └── event-publisher/           # Productor SQS con esquema versionado  [sqs.enabled]
│   └── helpers/       # CorrelationWebFilter, DataMasker, AesGcmCipher
└── application/app-service/  # MainApplication, config, application.yml, logback-spring.xml
```

## Variables de Entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| `SERVICE_NAME` | Nombre del servicio | `accounts-service` |
| `SERVER_PORT` | Puerto HTTP | `8080` |
| `DB_URL` / `DB_USER` / `DB_PASSWORD` | R2DBC PostgreSQL | — |
| `CREDIT_SCORE_URL` | Servicio externo de score | `https://credit-score.internal` |

Adaptadores opcionales se activan por bandera: `dynamodb.enabled`, `redis.enabled`, `sqs.enabled`.

## Prácticas del Framework de Madurez cubiertas

- **Errores**: RFC 7807 + extensiones `code`/`metadata`/`traceId`; negocio vs técnico; nunca expone internals.
- **API**: métodos HTTP correctos, códigos correctos, paginación con tope, OpenAPI, headers de seguridad.
- **Seguridad**: validación de entrada, CORS/headers, no-leak de errores.
- **Datos sensibles**: `@Sensitive`, masking en logs y respuestas, AES-256-GCM.
- **Resiliencia**: timeout + retry (backoff+jitter) + circuit breaker + bulkhead en el cliente externo; aislamiento de fallos en el consumidor SQS.
- **Observabilidad**: logs JSON, correlation id, health liveness/readiness, métricas Prometheus.
- **Config**: externalizada, perfiles, validada al arranque (fail fast).
- **Calidad**: pirámide de tests, ArchUnit, cobertura 90% gate, PIT.

Ver `docs/` para catálogo de errores, ADRs, threat model, glosario y runbook.

## Notas del Artefacto

- No usa `@RestController`, JPA, `RestTemplate`, `RestClient` ni Tomcat (reglas del stack WebFlux).
- No se permiten llamadas `.block()` en producción (BlockHound lo verifica en tests).
- Los adaptadores AWS/Redis están como muestra del patrón y se activan por configuración; no exigen la dependencia real para arrancar el servicio base.
