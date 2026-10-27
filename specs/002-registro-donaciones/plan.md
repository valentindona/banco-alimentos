# Implementation Plan: Registro de donaciones y comprobantes de recepción

**Branch**: `002-donaciones-facturacion` (active Git branch) | **Date**: 2026-10-26 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `specs/002-registro-donaciones/spec.md`

## Summary

Agregar el registro de donantes y donaciones recibidas con líneas auditables, sumar al
stock existente por producto y vencimiento, y emitir un comprobante inmutable ligado al
donante y a la donación. La confirmación coordina donación, líneas, stock y comprobante
en una transacción. Se amplían las features existentes de Producto, Stock, Factura y
Seguridad; no se reescriben sus reglas ni los flujos de remitos o vencimientos.

La inspección de la implementación actual confirma que `Factura` no tiene relación con
`Remito`: es una proyección de consulta que requiere `institucion_id` y expone fecha,
estado e importe. Para soportar comprobantes de recepción, una migración V7 aditiva
conservará esos valores para facturas anteriores, permitirá que las facturas nuevas no
tengan institución y añadirá vínculos opcionales con Donante y Donación. Una donación
confirmada tendrá exactamente un comprobante; se preservarán la consulta y los datos de
las facturas legadas.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot Web MVC, Data JPA, Validation, Security,
Hibernate Envers, Flyway, Lombok, ModelMapper; dependencias ya presentes.

**Storage**: MySQL 8.4 como base principal; H2 en modo MySQL para integración y pruebas.

**Testing**: Maven Wrapper, JUnit 5, Mockito, Spring Boot Test y H2; migraciones Flyway
ejecutadas en el contexto de pruebas.

**Target Platform**: Servicio web REST en el entorno existente del banco de alimentos.

**Project Type**: Backend monolítico organizado por features.

**Performance Goals**: Mantener consultas paginadas; verificar filtros con los criterios
de aceptación de la especificación, incluyendo una muestra de hasta 10.000 registros.

**Constraints**:
- Reutilizar Producto, Stock, autenticación/roles, `ReglaNegocioException`,
  `GlobalExceptionHandler`, Envers y las migraciones existentes.
- La confirmación debe ser atómica y conservar la separación entre features: Donación
  coordina mediante los services públicos de Stock y Factura, no accede a sus
  repositorios internos.
- Toda escritura nueva y todos los nuevos listados de esta funcionalidad quedan
  restringidos explícitamente a `user_deposito`.
- No se modifican los comportamientos existentes de remitos ni del control de
  vencimientos.
- Añadir solo migraciones posteriores a V6; probar las migraciones y endpoints con H2.
- Los controllers exponen DTOs, nunca entidades de persistencia.

**Scale/Scope**: Tres features nuevas de dominio (Donante, Donación e ItemDonacion);
ampliaciones puntuales de Producto, Stock, Factura, Seguridad y OpenAPI. Catálogo inicial
con al menos leche, harina y galletitas; demás tipos, unidades y productos quedan sujetos
a los supuestos enumerados en [research.md](./research.md).

## Constitution Check

### Antes del diseño

| Principio | Estado | Aplicación |
|-----------|--------|------------|
| I. Features verticales y dominio en español | PASS | Nuevos paquetes `donante`, `donacion` e `itemdonacion`; ampliaciones en los paquetes existentes. |
| II. Reglas de negocio en servicios y errores estandarizados | PASS | Servicio de confirmación valida donante, líneas, vencimientos y cantidades; usa `ReglaNegocioException` y el handler existente. |
| III. Seguridad y validación en el borde | PASS | DTOs con Bean Validation; ampliar configuración de seguridad para declarar acceso `user_deposito` a cada ruta nueva y de escritura existente. |
| IV. Integridad, auditoría, filtros y paginación | PASS | Envers para entidades nuevas y cambios de Producto/Factura; listados pageable con filtros; stock conserva la clave única producto-vencimiento. |
| V. Tests, OpenAPI y simplicidad | PASS | Pruebas unitarias e integración H2 antes/junto al código; contrato OpenAPI; sin dependencias nuevas. |
| MySQL de producción y H2 de pruebas | PASS | V7+ y catálogo seed compatibles con ambos perfiles; validación Flyway en H2. |

No se identifica una violación constitucional ni se requiere complejidad adicional
fuera de los servicios y límites de feature ya establecidos.

## Project Structure

### Documentation (this feature)

```text
specs/002-registro-donaciones/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── openapi.yaml
└── tasks.md             # Se genera en la fase /speckit-tasks
```

### Source Code (repository root)

```text
src/main/java/com/group6/BancoAlimentos/
├── donante/
│   ├── controller/
│   ├── dto/
│   ├── mapper/
│   ├── modelo/
│   ├── repository/
│   └── service/
├── donacion/
│   ├── controller/
│   ├── dto/
│   ├── mapper/
│   ├── modelo/          # Donacion
│   ├── repository/
│   └── service/
├── itemdonacion/
│   ├── controller/      # Consulta de líneas anidada bajo una donación
│   ├── dto/
│   ├── mapper/
│   ├── modelo/
│   ├── repository/
│   └── service/
├── producto/            # Se amplía la feature existente; no se reemplaza
├── stock/               # Se añade una operación de servicio de ingreso
├── factura/             # Se amplía el modelo/proyección existente
├── Security/            # Se amplían las reglas de acceso existentes
└── Config/              # Configuración existente de OpenAPI

src/main/resources/
├── db/migration/
│   └── V7__donantes_donaciones_y_comprobantes.sql
└── openapi.yaml         # Se sincroniza con el contrato de la feature

src/test/
├── java/com/group6/BancoAlimentos/{donante,donacion,itemdonacion,factura,producto,stock,Seguridad}/
└── resources/
    └── data.sql
```

**Structure Decision**: Mantener el proyecto Spring monolítico existente y sus paquetes
verticales. Donación es el servicio coordinador de la unidad de trabajo y llama a
`ItemDonacionService`, `StockService` y `FacturaService`; cada feature mantiene su propio
controller, service, repository, DTO y mapper conforme a la constitución. La consulta de
líneas es un subrecurso de solo lectura de una donación; las líneas se escriben solo
dentro de la operación atómica de confirmación. No se introduce una capa distribuida ni
se acoplan controllers o repositorios de features distintas.

## Diseño y secuencia propuesta

1. Añadir primero pruebas unitarias para donantes, producto/alimento, ItemDonacion,
   validación de líneas, incremento de stock por lote, comprobante, atomicidad,
   autorización y consultas/filtros.
2. Crear migración Flyway V7 compatible con las migraciones V1–V6: tablas auditadas
   `donantes`, `donaciones` e `items_donacion`; auditoría para la nueva columna de tipo
   de alimento en productos; y referencias opcionales de Factura a donante y donación.
   Incluir catálogo inicial idempotente o restringido a la ejecución única de Flyway.
3. Implementar cada feature en su paquete: Donante, Donación e ItemDonacion, cada una
   con controller, service, repository, DTOs y mapper. ItemDonacion expone solo lectura
   bajo la donación y conserva snapshot de nombre, tipo y unidad para el comprobante y
   el historial.
4. Extender Producto con tipo de alimento, edición y administración de catálogo,
   preservando sus consultas actuales y marcando como inactivos los productos que ya
   tengan referencias en stock/historial en vez de borrarlos físicamente.
5. Añadir a Stock un servicio público de ingreso que reutilice la existencia única por
   producto-vencimiento y sume cantidad; validar fecha futura y cantidades positivas
   antes de guardar. Para concurrencia de ingreso, serializar la resolución de un
   producto existente mediante el service/repositorio de Producto; conservar la
   restricción única existente como última defensa.
6. Extender Factura con vínculos a Donante y Donación, más lectura y creación interna
   del comprobante sin endpoints de edición/anulación. Una relación única factura-
   donación y el detalle inmutable de la donación mantienen la correspondencia.
7. Implementar la confirmación transaccional: resolver y validar todas las líneas,
   persistir donación e ítems mediante sus services, ingresar cada lote mediante
   StockService y crear una factura vía FacturaService. No capturar fallos como éxito;
   cualquier excepción de negocio o persistencia debe revertir la transacción completa.
8. Añadir endpoints CRUD/consulta permitidos por el alcance, listados paginados y
   filtros, reglas de acceso explícitas, documentación OpenAPI y pruebas de integración
   H2.
9. Ejecutar regresión completa, Flyway H2 limpio, contrato OpenAPI y verificar que las
   respuestas de errores usan el formato global existente.

## Riesgos, supuestos y decisiones que deben confirmarse en implementación

- **Factura actual**: V6 define `facturas.institucion_id NOT NULL` con FK, y el modelo
  Java también lo exige. No existe una FK a Remito ni a Donante/Donación. V7 debe
  permitir `institucion_id NULL` solo para los comprobantes nuevos; las filas y valores
  históricos permanecen sin cambios. Añadirá `donante_id` y `donacion_id` anulables
  para conservar también la posibilidad de consultar proyecciones legadas. Las tablas
  de auditoría de Factura deben incluir las columnas nuevas. Antes de fijar la sintaxis
  de alteración, probar la misma migración con Flyway y el SQL real de H2/MySQL.
- **Campos heredados de Factura**: la proyección existente exige `numero`, `fecha`,
  `estado` e `importe`; el comprobante de recepción no es fiscal ni monetario. Se
  propone conservar los valores históricos y, para los recibos nuevos, usar
  `estado=RECIBIDA` e `importe=0.00` como valores neutrales. No se derivará un importe
  económico ni se mostrará el comprobante como deuda/cobro. **Supuesto por validar**.
- **Numeración del comprobante**: la especificación no define formato ni consecutivo.
  Se propone generar un identificador de recepción a partir de la donación, sin alterar
  los números históricos. **Supuesto por validar antes de implementar unicidad/formato**.
- **Producto de catálogo modificado al recibir**: no cambiar la ficha original ni
  reinterpretar existencias ya registradas. Si se altera nombre, tipo o unidad para una
  recepción, se propone resolver/crear una identidad de Producto separada para que el
  stock mantenga una única unidad base; el ítem conserva además su snapshot. **Supuesto
  por validar**, especialmente para cambios solo de descripción o tipo.
- **Productos existentes sin tipo**: el modelo actual no guarda tipo de alimento. Para
  no asignar una clasificación inventada durante V7, los productos anteriores pueden
  quedar sin tipo hasta que depósito los clasifique; el servicio exige tipo completo
  para cualquier producto nuevo o recepción. El catálogo seed nuevo sí debe tener tipo.
  **Supuesto por validar** para productos históricos que se quieran recibir antes de
  clasificarlos.
- **Unidad y precisión**: el stock existente usa tres decimales. Se propone rechazar
  cantidades que no puedan representarse en esa precisión y no convertir unidades de
  manera automática; las conversiones no están especificadas. **Supuesto por validar**.
- **Catálogo inicial**: leche, harina y galletitas están requeridas; sus valores
  concretos de tipo/unidad y cualquier otro producto se deben cerrar como datos seed
  revisables, sin inferir un sistema fiscal o nutricional de clasificación. **Supuesto
  por validar**.
- **Baja de productos y donantes**: usar desactivación lógica para conservar claves
  foráneas, auditoría e historial. Un donante inactivo no admite nuevas donaciones.
- **Líneas repetidas del mismo lote**: conservar cada línea en el comprobante y sumar
  todas al único stock de producto-vencimiento. No se rechazarán líneas repetidas con
  la misma fecha, salvo que negocio prefiera validación estricta. **Supuesto por validar**.
- **Concurrencia**: H2 verifica atomicidad y reglas, no todas las garantías de bloqueo
  MySQL. La clave única existente protege el par producto-vencimiento; agregar una
  comprobación MySQL solo si hay `MYSQL_TEST_URL`, sin requerir Docker para las pruebas
  locales.

## Re-evaluación de Constitución

| Principio | Estado | Revisión del diseño |
|-----------|--------|---------------------|
| I. Features verticales y español | PASS | La coordinación cruza features solo por servicios; paquetes y símbolos del dominio en español. |
| II. Reglas y errores | PASS | `DonacionService` concentra la validación y la transacción; errores de negocio reutilizan `ReglaNegocioException` y el handler. |
| III. Seguridad y borde | PASS | Entradas en DTOs validadas; se especifica acceso de depósito explícito para cada método y se conservan JWT/roles existentes. |
| IV. Integridad y auditoría | PASS | V7 conserva historia, añade tablas de auditoría y no debilita la no-entrega de vencidos; listados filtrados/paginados. |
| V. Calidad y documentación | PASS | JUnit/Mockito/H2, OpenAPI sincronizado y ninguna dependencia nueva planeada. |
| Stack y límites de arquitectura | PASS | Java 21, Spring y Flyway actuales; se reutilizan código y modelos existentes; no hay nuevos módulos ni servicios. |

## Complexity Tracking

No hay violaciones constitucionales que justificar ni dependencias nuevas planificadas.

## Decisiones del equipo sobre los supuestos
- Importe y estado del comprobante: sin importe; estado fijo "emitida".
- Numeración: id autoincremental.
- Unidad del stock: una sola unidad base por producto; el ítem de la donación conserva lo cargado.
- Productos históricos: quedan "sin clasificar" y se completan a mano.
- Conversión de unidades: no se hacen conversiones.
- Clasificación: solo el tipo de alimento del catálogo.
- Mismo producto con el mismo vencimiento en una donación: se suman en un solo lote.
