# Implementation Plan: Entrega de mercadería con remitos y control de vencimientos

**Branch**: `001-remitos-vencimientos` | **Date**: 2026-10-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-remitos-vencimientos/spec.md`

## Summary

Crear desde cero una API REST Spring Boot para preparar y confirmar remitos de
instituciones beneficiarias, descontando en una única transacción el stock del producto
y vencimiento seleccionados. La aplicación incluirá las entidades mínimas de producto,
existencia por vencimiento e institución, autenticación JWT con los tres roles del
proyecto, consulta de facturas en modo solo lectura y avisos persistidos para depósito.
La fecha se comprobará en cada operación de entrega; una tarea diaria marcará existencias
vencidas y generará avisos idempotentes.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Versión estable mantenida de Spring Boot compatible con Java
21: Web MVC, Data JPA, Validation, Security, Hibernate Envers, jjwt, Lombok, ModelMapper
y Springdoc OpenAPI; Flyway para versionar el esquema, justificado por la persistencia
de datos y el despliegue reproducible desde cero.

**Storage**: MySQL para ejecución; H2 para pruebas de integración.

**Testing**: JUnit 5 y Mockito para reglas de negocio; pruebas de integración de endpoints
con Spring Boot Test/MockMvc y H2; ejecutar mediante `./mvnw` o `mvnw.cmd`.

**Target Platform**: Servicio web ejecutable en Java 21; desarrollo local con Docker
Compose y MySQL.

**Project Type**: API REST monolítica organizada por features.

**Performance Goals**: No se define objetivo de latencia o volumen para esta entrega.
Los listados serán paginados; el tamaño inicial de página se asumirá en 20 elementos,
con máximo configurable de 100.

**Constraints**: Los secretos se configuran por variables de entorno o archivos locales
ignorados por Git. La confirmación no reserva stock en borradores y debe validar y
descontar todas las líneas de forma atómica. El vencimiento se comprueba por fecha
inclusive; el trabajo diario no sustituye la validación de cada remito. No se modifica
la generación de donaciones ni de facturas.

**Scale/Scope**: Proyecto académico en equipo, iniciado desde cero. Incluye Remito,
DetalleRemito, Producto, existencia por vencimiento, Institucion, usuarios/roles,
notificaciones de depósito y consulta de facturas de solo lectura. Las otras reglas de
donación y facturación quedan fuera de alcance.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

**Initial gate: PASS**

- Arquitectura por features bajo `com.group6.BancoAlimentos`; cada feature tendrá
  controller, service, repository, modelo, DTOs y mapper. Las dependencias entre
  features se resolverán mediante services.
- Toda la lógica de negocio y validación de vencimientos/stock vive en services.
  `ReglaNegocioException` se mapeará a HTTP 422 y `GlobalExceptionHandler` devolverá un
  formato común de error.
- Endpoints REST bajo `/api/...`; controllers intercambian DTOs validados con `@Valid`.
  Cada ruta declara su política explícita de roles.
- Hibernate Envers audita las entidades de negocio. La entrega y sus reglas tendrán
  tests unitarios y de integración en H2.
- Java 21, Spring Boot, Maven Wrapper, MySQL, H2, Lombok, ModelMapper, JWT, Springdoc y
  Docker Compose cumplen las restricciones del proyecto. Flyway es la única dependencia
  adicional propuesta, para administrar cambios de esquema reproducibles.
- No se detectan violaciones constitucionales que requieran una excepción.
- **Post-design gate: PASS**. El modelo, contratos y quickstart mantienen la separación
  entre features, documentan roles por operación, conservan DTOs y traducen las reglas
  de dominio a HTTP 422; tests, auditoría y OpenAPI están incluidos en el diseño.

## Project Structure

### Documentation (this feature)

```text
specs/001-remitos-vencimientos/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── openapi.yaml
│   └── errores.md
└── tasks.md
```

### Source Code (repository root)

```text
pom.xml
mvnw
mvnw.cmd
.gitignore
docker-compose.yml
src/
├── main/
│   ├── java/com/group6/BancoAlimentos/
│   │   ├── Common/
│   │   │   ├── excepcion/
│   │   │   ├── handler/
│   │   │   └── respuesta/
│   │   ├── Config/
│   │   ├── Security/
│   │   ├── usuario/{controller,service,repository,modelo,dto,mapper}/
│   │   ├── producto/{controller,service,repository,modelo,dto,mapper}/
│   │   ├── stock/{controller,service,repository,modelo,dto,mapper}/
│   │   ├── institucion/{controller,service,repository,modelo,dto,mapper}/
│   │   ├── remito/{controller,service,repository,modelo,dto,mapper}/
│   │   ├── detalleremito/{controller,service,repository,modelo,dto,mapper}/
│   │   ├── factura/{controller,service,repository,modelo,dto,mapper}/
│   │   └── notificacion/{controller,service,repository,modelo,dto,mapper}/
│   └── resources/
│       ├── application.yml
│       └── db/migration/
└── test/
    ├── java/com/group6/BancoAlimentos/
    │   ├── unit/
    │   └── integration/
    └── resources/application-test.yml
```

**Structure Decision**: Un único proyecto Maven, sin módulos Maven separados. Cada área
funcional tiene paquetes verticales propios; Common, Config y Security contienen solo
responsabilidades compartidas. Los módulos se comunicarán mediante interfaces de service;
no expondrán ni importarán entidades internas ni repositories ajenos. Factura será
estrictamente de consulta para satisfacer
el permiso de depósito de la especificación; no tendrá operaciones de generación,
cobro ni modificación. Datos de catálogo/stock/instituciones para pruebas y demo se
provisionarán fuera del flujo de donaciones, que queda para otra spec.

## Assumptions and Scope Boundaries

- El módulo mínimo de consulta de facturas tendrá una proyección de solo lectura con
  número, institución, fecha, estado e importe. Su esquema definitivo debe alinearse con
  el futuro diseño de facturación; este plan no agrega creación ni lógica financiera.
- Para la primera ejecución, los registros de productos, instituciones y stock se
  cargarán como datos de arranque/demo. El ingreso de donaciones y su efecto sobre stock
  pertenecen a una spec futura; no se agrega un flujo de donación ni de recepción.
- Las cuentas mínimas se provisionarán mediante configuración de entorno/arranque, sin
  registro público. Las contraseñas se almacenarán codificadas y no en texto plano.
- Las notificaciones persistidas serán compartidas por el rol de depósito y se crearán
  una sola vez por existencia y tipo (próximo a vencer o vencida); no se requiere marcar
  como leídas, porque la especificación excluye confirmación de lectura.
- La existencia por vencimiento, y no el producto de catálogo completo, es lo que se
  bloquea al vencer. El service rechazará stock cuya fecha sea hoy o anterior incluso
  si la tarea diaria aún no actualizó su estado.
- Las cantidades se manejarán como decimales positivos con unidad definida por producto;
  los datos incompletos de Institucion se limitarán a los campos mínimos fijados en el
  modelo de esta feature.
- La tarea programada correrá diariamente a medianoche con zona horaria configurable;
  se asume `America/Argentina/Buenos_Aires` como valor inicial.
- La entrega es una API sin interfaz web incluida en esta feature; el cliente de
  depósito consume el endpoint de notificaciones para presentarlas en pantalla.
- Las notificaciones se comparten por rol de depósito, sin estado de lectura ni
  vencimiento automático. Una clave única por existencia y evento evita duplicados aun
  si la tarea se reintenta; se asume una instancia de aplicación para esta entrega.
- La cuenta inicial de cada rol se crea solo mediante un proceso protegido de
  provisionamiento. En producción, el secreto JWT es obligatorio y la aplicación falla
  al iniciar si falta; no habrá claves ni contraseñas conocidas por defecto. Se asume
  una clave de firma criptográficamente aleatoria de al menos 256 bits.
- Las entidades de negocio Producto, Stock, Institucion, Remito, DetalleRemito y
  Factura se auditan con Envers. Usuario y Notificacion se consideran datos
  operativos/de seguridad y no se incluyen en la auditoría de negocio.
- Confirmar bloqueará el remito y las existencias requeridas en orden estable, validará
  todas las líneas y solo entonces aplicará todos los descuentos dentro de la misma
  transacción. No habrá reserva durante el borrador.
- La regla de producto vencido de la especificación prevalece: la fecha de vencimiento
  es inclusiva como vencida (`fechaVencimiento <= fechaNegocio`), aunque algunas
  convenciones de inventario traten el día indicado como último día hábil.
- Flyway versionará el esquema. Los datos de demostración serán de perfil local/test;
  no se sembrarán usuarios ni secretos de producción automáticamente.

## Complexity Tracking

No hay violaciones constitucionales ni componentes separados innecesarios. Flyway se
considera justificado para crear y evolucionar el esquema persistente de forma reproducible.
