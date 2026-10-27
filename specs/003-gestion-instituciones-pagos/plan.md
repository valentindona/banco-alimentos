# Implementation Plan: Gestión de instituciones y estados de pago

**Branch**: `003-gestion-instituciones-pagos` | **Date**: 2026-10-26 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/003-gestion-instituciones-pagos/spec.md` and user guidance to reuse the existing institution feature, security roles, business exceptions, auditing, migrations, and H2 tests.

## Summary

Completar la feature existente de Institución con altas, consultas, modificaciones y bajas lógicas para el rol institucional; permitir a tesorería consultar y cambiar el estado de pago; y permitir a depósito consultar. Se ampliarán los filtros paginados, se documentarán los endpoints en OpenAPI y se probarán reglas, permisos y migraciones con H2. Se conservarán la entidad `Institucion`, su auditoría Envers, las relaciones actuales de remitos y los componentes compartidos existentes.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1 (Web MVC, Data JPA, Validation, Security), Hibernate Envers, Flyway, Lombok, the shared ModelMapper configuration, Springdoc OpenAPI

**Storage**: MySQL en producción; H2 en pruebas, usando Flyway y `ddl-auto: validate`

**Testing**: Maven Wrapper (`mvnw`), JUnit 5, Mockito, pruebas de integración Spring MVC/JPA con H2

**Target Platform**: API REST Spring Boot

**Project Type**: Servicio web backend

**Performance Goals**: Mantener paginación existente, por defecto 20 registros y máximo 100 por página; no se define un objetivo de latencia adicional.

**Constraints**: Respetar arquitectura por feature, nombres de dominio en español, roles existentes, respuestas y errores comunes, auditoría Envers, endpoints OpenAPI y migraciones compatibles con MySQL/H2. Las bajas son lógicas para conservar las referencias de remitos.

**Scale/Scope**: Una entidad de institución; filtros por nombre, tipo y estado de pago; endpoints de lectura y escritura bajo `/api/instituciones`.

## Constitution Check

### Pre-Phase 0

- **Organización por feature y dominio**: PASS. Se amplía `institucion` bajo `com.group6.BancoAlimentos` con controller, services, repository, modelo, DTOs y mapper propios; el mapper utiliza la configuración compartida de ModelMapper.
- **Reglas de negocio y errores**: PASS. Las reglas se validarán en services y usarán `ReglaNegocioException` para rechazos de negocio, con el `GlobalExceptionHandler` existente.
- **Seguridad y validación**: PASS. Se reutilizan JWT y los roles `user_institucional`, `user_tesoreria` y `user_deposito`; los DTOs de entrada se validan en el borde.
- **Integridad y auditoría**: PASS. No se elimina físicamente la institución; los cambios a la entidad existente continúan bajo Envers. Los listados son filtrables y paginados.
- **Testing, documentación y estilo**: PASS. Se agregarán pruebas unitarias/integración con JUnit 5, Mockito y H2, además del contrato OpenAPI.
- **Stack y migraciones**: PASS. No se agregan dependencias; los cambios de esquema se incorporan en una nueva migración posterior a V7 y se validan con ambos motores.

No se identifican violaciones que requieran justificación.

## Project Structure

### Documentation (this feature)

```text
specs/003-gestion-instituciones-pagos/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md
```

### Source Code (repository root)

```text
src/
├── main/
│   ├── java/com/group6/BancoAlimentos/
│   │   ├── institucion/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── mapper/
│   │   │   ├── modelo/
│   │   │   ├── repository/
│   │   │   └── service/
│   │   ├── Security/ConfiguracionSeguridad.java
│   │   └── Common/
│   │       ├── excepcion/ReglaNegocioException.java
│   │       └── handler/GlobalExceptionHandler.java
│   └── resources/
│       ├── db/migration/V8__...sql
│       └── openapi.yaml
└── test/
    ├── java/com/group6/BancoAlimentos/
    │   ├── institucion/
    │   └── Seguridad/
    └── resources/application-test.yml
```

**Structure Decision**: Se sigue la estructura backend existente. La feature ya tiene entidad, repository, controller y consultas de Institución; se extiende esa feature y se actualizan las reglas centrales de seguridad y el contrato OpenAPI publicado.

## Assumptions

- **Decisión confirmada**: `tipo` será texto libre de hasta 80 caracteres, sin catálogo cerrado. La entidad y la columna permiten `null` para mantener compatibilidad con instituciones históricas; la solicitud de alta/modificación conserva la validación obligatoria del contrato.
- Una nueva migración V8 agregará el campo a `instituciones` y `instituciones_aud`. Las instituciones preexistentes recibirán el valor `Sin especificar`, para que el campo requerido no impida la migración ni invalide datos existentes.
- **Decisión confirmada**: una institución nueva comenzará con estado `AL_DIA`; tesorería será el único rol que podrá modificarlo después.
- **Decisión confirmada**: el rol `user_institucional` podrá administrar todas las instituciones, sin restricción a la institución asociada al usuario.
- Los nombres se considerarán duplicados sin distinguir mayúsculas/minúsculas; se validarán en el service para nuevas altas y modificaciones, sin requerir reescritura de registros existentes.
- La baja será lógica (`activa = false`); por defecto, las consultas operativas listarán solo instituciones activas. La desactivación preservará usuarios, remitos y revisiones históricas relacionados.
- Los filtros múltiples se combinarán con AND; nombre usará coincidencia parcial sin distinguir mayúsculas/minúsculas, y tipo coincidirá exactamente luego de normalizar espacios exteriores.
- Se mantienen los errores del proyecto: validación de entrada en HTTP 400, falta de autenticación en 401, falta de permisos en 403 y reglas de negocio mediante HTTP 422.

## Constitution Check (Post-Design)

- **Arquitectura**: PASS. El diseño no crea una feature duplicada ni cruza límites de dominio; controller delega en servicios y el mapper adapta DTOs.
- **Reglas de negocio**: PASS. Alta/actualización, duplicidad, búsqueda y baja se validan en services; se reutilizan excepciones y manejador global.
- **Seguridad**: PASS. Se definen permisos explícitos por método y ruta. Consulta para depósito, tesorería e institucional; ABM para institucional; estado de pago para tesorería.
- **Integridad y auditoría**: PASS. La baja lógica conserva las claves foráneas de remitos y el campo nuevo se agrega también a la tabla de auditoría.
- **Testing y documentación**: PASS. El contrato especifica cada endpoint y rol; quickstart prescribe pruebas focalizadas e integración H2.
- **Migración**: PASS con supuesto documentado. La migración V8 hace backfill del campo nuevo antes de imponer obligatoriedad, y se ejecuta en MySQL y H2.

No quedan decisiones técnicas bloqueantes ni violaciones constitucionales sin resolver. El valor inicial `AL_DIA`, el tipo de texto libre y el alcance global del rol institucional fueron confirmados por el usuario. El texto de backfill `Sin especificar` sigue siendo un supuesto operativo para preservar los registros actuales.

## Complexity Tracking

No se agregan dependencias ni se identifican excepciones a la constitución.
