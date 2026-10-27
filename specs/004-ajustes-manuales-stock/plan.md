# Plan de implementación: Ajustes manuales de stock

**Branch**: `004-ajustes-manuales-stock` | **Date**: 2026-10-27 | **Spec**: [spec.md](./spec.md)

## Summary

Agregar una feature vertical `ajuste` para que exclusivamente `user_deposito` fije la cantidad real de un lote o dé de baja una cantidad, y consulte un historial paginado, filtrable e inmutable. El service ajustará la entidad `Stock` existente y persistirá el evento de negocio dentro de una sola transacción, luego de bloquear la fila del lote con `PESSIMISTIC_WRITE`. Se reutilizan las excepciones, el handler, el esquema de seguridad, Envers y las convenciones de migración ya presentes; no se reescriben los servicios existentes de stock, donaciones ni remitos.

Un remito `BORRADOR` no reserva stock: se permite ajustar un lote incluido en él. La confirmación ya vuelve a bloquear y validar los lotes y sus cantidades; si el ajuste dejó disponibilidad insuficiente, debe rechazarse la confirmación y conservarse el borrador sin descontar stock.

## Technical Context

**Language/Version**: Java 21.
**Primary Dependencies**: Spring Boot 4.1.1, Spring Web MVC, Spring Data JPA, Bean Validation, Spring Security/JWT, Hibernate Envers, Flyway, Lombok, ModelMapper, Springdoc; todas ya están declaradas.
**Storage**: MySQL en producción; H2 en modo MySQL para pruebas.
**Testing**: Maven Wrapper, JUnit 5, Mockito y pruebas de integración con H2 según el patrón existente.
**Target Platform**: API REST Spring Boot.
**Project Type**: Backend monolítico organizado por features verticales bajo `com.group6.BancoAlimentos`.
**Performance Goals**: No hay SLA ni volumen objetivo definido. Supuesto de planificación: reutilizar paginación con tamaño máximo 100 y los índices de consulta descritos en el diseño; no introducir objetivos de latencia no requeridos.
**Constraints**: Respetar DTOs en la API, lógica de negocio en services, autorización explícita, transacción atómica y cantidades `DECIMAL(12,3)` no negativas. Mantener compatibilidad con migraciones V1–V8 y con la confirmación actual de remitos.
**Scale/Scope**: Una entidad de negocio nueva, dos operaciones HTTP y un listado paginado; no se agregan dependencias ni se cambia el modelo general de inventario.

### Supuestos que requieren validación durante implementación

- La API identificará un lote mediante `stockId`, identificador ya expuesto por las respuestas de stock. Esto permite ajustar también lotes existentes cuyo vencimiento sea nulo, sin ambigüedad, y reutiliza el bloqueo pesimista existente por ID. El evento conserva además producto y vencimiento como instantánea.
- El parámetro de usuario del filtro será `usuarioId`; el rango `fechaDesde`/`fechaHasta` será inclusivo sobre la fecha UTC de `fechaHora`. No se especifica zona horaria de negocio.
- La cantidad solicitada será una cantidad positiva para `BAJA` y la cantidad absoluta objetivo (cero permitido) para `CORRECCION`; ambas respetan hasta tres decimales. El motivo se recorta al inicio y al final.
- No se añadirá un límite de longitud para el motivo que no esté fijado por un requisito; usar un campo de texto compatible y confirmarlo al implementar. Un lote existente de un producto inactivo podrá ajustarse, salvo que se acuerde una regla de negocio adicional.
- La inmutabilidad aplica a la API y al modelo de escritura de la aplicación: no existirán rutas ni métodos de repositorio de modificación o borrado. No se añadirán triggers ni permisos especiales de base de datos salvo que el equipo requiera protección frente a escrituras SQL externas.
- No se cambia el estado del lote al ajustar: baja y corrección alteran solo su cantidad; se conserva el estado actual. La elegibilidad para remitos sigue dependiendo de las validaciones existentes de estado y vencimiento.
- Los IDs de producto, usuario y stock son referencias a registros existentes; los datos de producto/vencimiento del ajuste se guardan como instantánea para preservar el contexto histórico incluso si cambian los datos relacionados.

## Constitution Check

*GATE: se evalúa antes de investigación y nuevamente después del diseño.*

| Principio | Resultado | Aplicación |
|---|---|---|
| I. Arquitectura por features y dominio en español | PASS | Feature `ajuste` en paquete base actual con controller, service, repository, modelo, DTOs y mapper. No se accede a internals de otra feature; el lote se opera mediante `StockRepository`, y el usuario autenticado se obtiene de `UsuarioAutenticado`. |
| II. Reglas de negocio y errores estandarizados | PASS | Validaciones de tipo, cantidades, lote y saldo en service con `ReglaNegocioException`; validación de borde en DTOs; errores manejados por `GlobalExceptionHandler`; controllers solo reciben DTOs y delegan. |
| III. Seguridad y validación en el borde | PASS | Reglas explícitas para ambos métodos en `ConfiguracionSeguridad`, solo `user_deposito`; `@Valid` para la solicitud; autor/fecha no provienen del cuerpo. |
| IV. Integridad y auditoría | PASS | Bloqueo pesimista y transacción común para lote e historial; registro append-only con Envers; migración con FK, restricciones e índices; filtros combinables y paginación; tests de reglas y endpoints con H2. |
| V. Testing, documentación y estilo | PASS | JUnit 5/Mockito, integración H2, contratos y anotaciones OpenAPI; nombres y mensajes en español. |
| Stack y simplicidad | PASS | Se usan las dependencias, excepciones, paginación, bloqueo y respuesta paginada existentes. No se agregan librerías ni servicios alternativos. |

**Violaciones**: ninguna identificada. La actualización existente del remito no necesita reescritura: su confirmación ya toma bloqueos ordenados y valida disponibilidad en la transacción.

## Project Structure

### Documentación de esta feature

```text
specs/004-ajustes-manuales-stock/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
└── contracts/
    └── ajustes-stock.openapi.yaml
```

### Código fuente (raíz del repositorio)

```text
src/main/java/com/group6/BancoAlimentos/ajuste/
├── controller/
├── dto/
├── mapper/
├── modelo/
├── repository/
└── service/

src/main/java/com/group6/BancoAlimentos/
├── Security/ConfiguracionSeguridad.java        # autorizar GET/POST /api/ajustes
└── stock/repository/StockRepository.java        # reutilizar bloqueo por id existente

src/main/resources/db/migration/
└── V9__crear_ajustes_stock.sql

src/main/resources/openapi.yaml                  # documentar rutas en el contrato publicado
src/test/java/com/group6/BancoAlimentos/ajuste/
├── controller/                                  # integración HTTP, H2 y autorización
├── repository/                                  # filtros/Envers si aplica
└── service/                                     # reglas, atomicidad y concurrencia
```

**Structure Decision**: El proyecto es un único servicio Spring Boot y las features existentes siguen paquetes verticales en español. La tabla de auditoría de Envers se declara en la migración Flyway, igual que las entidades de negocio existentes. Se añadirá una sección equivalente al contrato de diseño en `src/main/resources/openapi.yaml`; el fragmento de `contracts/` fija el contrato previsto durante la implementación.

## Phase 0 — Research

Las decisiones, razones, alternativas y fuentes se detallan en [research.md](./research.md). Se resuelven los patrones de bloqueo/transacción, registro de negocio frente a Envers, esquema de filtros y decisión de remitos en borrador. El único SLA no definido permanece como supuesto de planificación, no como bloqueo de diseño.

## Phase 1 — Design

- Entidad, validaciones y ciclo de vida: [data-model.md](./data-model.md).
- Contrato HTTP y shapes de DTO/respuesta: [contracts/ajustes-stock.openapi.yaml](./contracts/ajustes-stock.openapi.yaml).
- Escenarios reproducibles de validación: [quickstart.md](./quickstart.md).
- Los DTOs de solicitud diferencian el significado de `cantidad` por tipo; autor, fecha y snapshot del lote los determina el service.
- El ajuste bloquea la fila de `Stock` existente antes de leer el saldo. Actualización del lote e inserción del registro pertenecen a la misma transacción; cualquier excepción revierte ambas.
- La consulta filtra conjuntamente por producto, vencimiento, fecha UTC, usuario y tipo, y responde con `PaginaRespuesta`.
- No se agregan endpoints de edición o borrado. La auditoría de negocio y la auditoría técnica Envers son complementarias.

### Revisión post-diseño de Constitution Check

**PASS**: el diseño mantiene límites de feature, delega autorización a la configuración central existente, usa DTOs y errores estándar, transacción y bloqueo para integridad, Envers y migración Flyway, paginación compartida, OpenAPI y tests H2. No requiere dependencias ni cambios en servicios de stock, donaciones o remitos. Ninguna desviación necesita excepción ni seguimiento.

## Complexity Tracking

No hay violaciones de la constitución ni complejidad adicional que justificar.
