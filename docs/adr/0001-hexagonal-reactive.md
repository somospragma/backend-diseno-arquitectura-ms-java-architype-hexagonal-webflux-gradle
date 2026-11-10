# ADR 0001: Arquitectura hexagonal reactiva

## Estado
Aceptado.

## Contexto
Necesitamos un servicio backend mantenible, testeable y no bloqueante, alineado al estándar Pragma WebFlux.

## Decisión
Adoptar arquitectura hexagonal multi-módulo (domain/ports/usecases + infrastructure + application) con Spring WebFlux (Netty) y Project Reactor. El dominio no depende de framework; los adaptadores implementan puertos.

## Consecuencias
- (+) Dominio testeable sin contenedor; cambio de infraestructura sin tocar negocio.
- (+) Reglas verificables con ArchUnit (regla de dependencia, no @RestController).
- (−) Curva de aprendizaje reactiva; prohibido bloquear el event loop (BlockHound lo verifica).
