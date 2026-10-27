---

description: "Tareas de implementación para la gestión de instituciones y estados de pago"
---

# Tasks: Gestión de instituciones y estados de pago

**Input**: Design documents from `specs/003-gestion-instituciones-pagos/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`

**Tests**: Se incluyen pruebas porque la especificación, la constitución del proyecto y la solicitud requieren verificar reglas, permisos, migraciones y contratos. En cada fase, las tareas de prueba preceden a la implementación que verifican.

**Organization**: Las historias siguen el orden pedido: base de datos, ABM, estado de pago y permisos de tesorería, consultas y filtros.

## Phase 1: Setup

**Purpose**: Preparación compartida del proyecto.

El proyecto Spring Boot, Maven Wrapper, Flyway, H2 de pruebas y la estructura de la feature ya existen; no se requieren tareas de inicialización.

---

## Phase 2: Foundational - Base de datos

**Purpose**: Incorporar el campo requerido por las historias antes de implementar las operaciones.

- [ ] T001 Agregar en `src/test/java/com/group6/BancoAlimentos/institucion/modelo/InstitucionMigracionTest.java` una prueba H2 que parta del esquema V7 con una institución existente, ejecute V8 y compruebe el backfill `Sin especificar`, la obligatoriedad de `tipo` y la columna correspondiente en `instituciones_aud`.
- [ ] T002 Crear `src/main/resources/db/migration/V8__agregar_tipo_a_instituciones.sql` para agregar `tipo` a `instituciones` y `instituciones_aud`, completar las instituciones existentes con `Sin especificar` y hacer obligatorio el campo; agregar el campo `tipo` a `src/main/java/com/group6/BancoAlimentos/institucion/modelo/Institucion.java` como texto no nulo de hasta 80 caracteres.

**Checkpoint**: La migración V8 y el mapeo JPA quedan validados en H2 antes de empezar las historias.

---

## Phase 3: User Story 1 - Administrar instituciones (Priority: P1)

**Goal**: Permitir al rol institucional crear, consultar, modificar y dar de baja lógicamente instituciones, conservando remitos e historial.

**Independent Test**: Con una cuenta `user_institucional`, crear una institución válida y comprobar su respuesta/listado con estado inicial `AL_DIA`; modificarla, verificar que un nombre duplicado se rechaza y darla de baja sin perder remitos históricos.

### Tests for User Story 1

- [ ] T003 [P] [US1] Agregar pruebas unitarias en `src/test/java/com/group6/BancoAlimentos/institucion/service/InstitucionServiceTest.java` para alta, actualización, rechazo de nombres duplicados sin distinguir mayúsculas/minúsculas, rechazo de institución inexistente o inactiva, baja lógica repetible y estado inicial `AL_DIA`.
- [ ] T004 [P] [US1] Agregar pruebas de integración MVC/JPA en `src/test/java/com/group6/BancoAlimentos/institucion/InstitucionControllerIntegracionTest.java` para POST, GET por id, PUT y DELETE; verificar validación HTTP 400, errores de negocio HTTP 422, control de acceso institucional y conservación de remitos luego de la baja.
- [ ] T005 [P] [US1] Agregar `src/test/java/com/group6/BancoAlimentos/Seguridad/ContratoInstitucionesOpenApiTest.java` para comprobar en `src/main/resources/openapi.yaml` las rutas y métodos de alta, consulta, actualización y baja, sus esquemas de solicitud/respuesta y roles declarados.

### Implementation for User Story 1

- [ ] T006 [US1] Crear `src/main/java/com/group6/BancoAlimentos/institucion/dto/InstitucionSolicitud.java`, actualizar `src/main/java/com/group6/BancoAlimentos/institucion/dto/InstitucionRespuesta.java` y agregar `src/main/java/com/group6/BancoAlimentos/institucion/mapper/InstitucionMapper.java`; validar nombre «obligatorio, máximo 150 caracteres», domicilio «obligatorio, máximo 250 caracteres» y tipo «obligatorio, máximo 80 caracteres; texto libre confirmado por el usuario, normalizado quitando espacios exteriores»; excluir estado de pago e indicador de activa de la solicitud.
- [ ] T007 [US1] Crear `src/main/java/com/group6/BancoAlimentos/institucion/service/InstitucionService.java` con alta, actualización y baja lógica; agregar en `src/main/java/com/group6/BancoAlimentos/institucion/repository/InstitucionRepository.java` las búsquedas requeridas para validar nombres duplicados ignorando mayúsculas/minúsculas y operar solo sobre instituciones activas; reutilizar `ReglaNegocioException`.
- [ ] T008 [US1] Extender `src/main/java/com/group6/BancoAlimentos/institucion/controller/InstitucionController.java` con POST, GET por id, PUT y DELETE, delegando en los servicios y exponiendo solo DTOs; actualizar `src/main/java/com/group6/BancoAlimentos/Security/ConfiguracionSeguridad.java` para autorizar el ABM al rol `user_institucional` y permitirle GET.
- [ ] T009 [P] [US1] Documentar los endpoints y esquemas de ABM en `src/main/resources/openapi.yaml`, reflejando las validaciones de los campos, los roles institucionales, las respuestas HTTP y la baja lógica definida en `specs/003-gestion-instituciones-pagos/contracts/openapi.yaml`.

**Checkpoint**: El ABM institucional está disponible y puede probarse sin implementar todavía el cambio de estado de pago ni los nuevos filtros.

---

## Phase 4: User Story 2 - Consultar y actualizar estados de pago (Priority: P1)

**Goal**: Permitir a tesorería consultar y modificar exclusivamente el estado vigente; repetir el valor actual debe ser válido y la deuda no debe bloquear remitos.

**Independent Test**: Con una cuenta `user_tesoreria`, cambiar el estado de una institución, comprobarlo en una consulta posterior, repetir el mismo estado y registrar un remito aunque la institución esté `EN_DEUDA`; comprobar que los otros roles no pueden cambiarlo.

### Tests for User Story 2

- [ ] T010 [P] [US2] Agregar pruebas unitarias en `src/test/java/com/group6/BancoAlimentos/institucion/service/InstitucionServiceTest.java` para actualizar cada valor permitido de `EstadoPago`, conservar sin error el estado seleccionado nuevamente y rechazar instituciones inexistentes o inactivas.
- [ ] T011 [P] [US2] Agregar pruebas MVC/JPA en `src/test/java/com/group6/BancoAlimentos/institucion/InstitucionEstadoPagoIntegracionTest.java` para PATCH, persistencia del cambio, repetición idempotente, rechazo de entrada inválida, acceso exclusivo de `user_tesoreria` y creación de remitos para una institución `EN_DEUDA`.
- [ ] T012 [P] [US2] Extender `src/test/java/com/group6/BancoAlimentos/Seguridad/ContratoInstitucionesOpenApiTest.java` para verificar el contrato PATCH de estado de pago, el cuerpo limitado al enum existente y el rol exclusivo `user_tesoreria`.

### Implementation for User Story 2

- [ ] T013 [US2] Implementar en `src/main/java/com/group6/BancoAlimentos/institucion/service/InstitucionService.java` la operación de cambio de estado de pago para instituciones activas, sin incluir el estado en los DTOs de alta/modificación y aceptando como operación válida seleccionar el valor ya vigente.
- [ ] T014 [US2] Agregar a `src/main/java/com/group6/BancoAlimentos/institucion/controller/InstitucionController.java` el endpoint PATCH `/api/instituciones/{institucionId}/estado-pago`, validando el DTO de solicitud y devolviendo el DTO de institución actualizado.
- [ ] T015 [US2] Actualizar `src/main/java/com/group6/BancoAlimentos/Security/ConfiguracionSeguridad.java` para autorizar el PATCH de estado de pago únicamente a `user_tesoreria`, sin conceder esa operación a `user_institucional` ni `user_deposito`.
- [ ] T016 [P] [US2] Documentar PATCH, su esquema de solicitud/respuesta, el enum `AL_DIA`, `EN_DEUDA`, `PARTICULAR` y los permisos en `src/main/resources/openapi.yaml`, de acuerdo con `specs/003-gestion-instituciones-pagos/contracts/openapi.yaml`.

**Checkpoint**: Tesorería puede mantener el estado de pago y las reglas de remitos siguen sin depender de ese estado.

---

## Phase 5: User Story 3 - Consultar instituciones por área (Priority: P2)

**Goal**: Permitir a institucional, tesorería y depósito consultar las instituciones activas con filtros combinables por nombre, tipo y estado de pago, sin conceder permisos de modificación a depósito.

**Independent Test**: Con cuentas de depósito y tesorería, combinar filtros y comprobar que cada resultado cumple todos los criterios; confirmar respuesta vacía cuando no hay coincidencias, paginación con tamaño por defecto 20 y máximo 100, y denegación de cambios para depósito.

### Tests for User Story 3

- [ ] T017 [P] [US3] Extender `src/test/java/com/group6/BancoAlimentos/institucion/service/ConsultaInstitucionServiceTest.java` y `src/test/java/com/group6/BancoAlimentos/institucion/modelo/InstitucionRepositoryTest.java` para filtros combinados AND por nombre parcial sin distinguir mayúsculas/minúsculas, tipo exacto normalizado y estado; verificar orden por id, exclusión de inactivas, resultados vacíos y límites de paginación.
- [ ] T018 [P] [US3] Agregar pruebas de integración en `src/test/java/com/group6/BancoAlimentos/institucion/InstitucionConsultaIntegracionTest.java` para GET de listado y detalle como `user_deposito`, `user_tesoreria` y `user_institucional`; comprobar acceso denegado sin cambios al intentar operaciones de escritura con depósito.
- [ ] T019 [P] [US3] Extender `src/test/java/com/group6/BancoAlimentos/Seguridad/ContratoInstitucionesOpenApiTest.java` para verificar los parámetros `nombre`, `tipo`, `estadoPago`, `pagina` y `tamano`, los roles habilitados para consultas y el formato paginado de respuesta.

### Implementation for User Story 3

- [ ] T020 [US3] Extender `src/main/java/com/group6/BancoAlimentos/institucion/repository/InstitucionRepository.java` y `src/main/java/com/group6/BancoAlimentos/institucion/service/ConsultaInstitucionService.java` para filtrar instituciones activas por nombre parcial sin distinción de mayúsculas/minúsculas, tipo exacto tras quitar espacios exteriores y estado, combinando los criterios con AND y ordenando por id ascendente.
- [ ] T021 [US3] Extender `src/main/java/com/group6/BancoAlimentos/institucion/controller/InstitucionController.java` para recibir el parámetro opcional `tipo` y mantener paginación común con 20 elementos por defecto y máximo 100; actualizar `src/main/java/com/group6/BancoAlimentos/Security/ConfiguracionSeguridad.java` para permitir GET de listado/detalle a `user_institucional`, `user_tesoreria` y `user_deposito`.
- [ ] T022 [P] [US3] Actualizar `src/main/resources/openapi.yaml` con los parámetros de filtros, valores por defecto/límites de paginación y roles autorizados para las consultas, manteniendo el contrato de `specs/003-gestion-instituciones-pagos/contracts/openapi.yaml`.

**Checkpoint**: Cada área puede consultar el padrón que le corresponde; los filtros se combinan y depósito mantiene acceso de solo lectura.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificar la integración de las historias y mantener alineada la guía de validación.

- [ ] T023 [P] Actualizar los nombres/selectores de pruebas y escenarios manuales de `specs/003-gestion-instituciones-pagos/quickstart.md` para reflejar las clases implementadas y ejecutar los comandos de validación focalizada indicados allí.
- [ ] T024 [P] Revisar que `src/main/resources/openapi.yaml` describa todos los endpoints, respuestas, validaciones y permisos acordados en `specs/003-gestion-instituciones-pagos/contracts/openapi.yaml`, y que los cambios no alteren las operaciones existentes de remitos.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No requiere inicialización adicional; el proyecto y sus herramientas ya existen.
- **Foundational (Phase 2)**: Ejecutar primero T001 y luego T002; la migración y el mapeo de `tipo` son prerequisitos de todas las historias.
- **User Story 1 (Phase 3)**: Depende de la base de datos; completa el ABM institucional antes de continuar con tesorería.
- **User Story 2 (Phase 4)**: Depende de la base de datos y de la institución/servicios/DTOs creados por US1.
- **User Story 3 (Phase 5)**: Depende de la base de datos y de los DTOs/consultas existentes y actualizados por US1 y US2.
- **Polish (Phase 6)**: Depende de las historias que se decida entregar.

### User Story Dependencies

- **US1 (P1)**: Después de la migración; MVP funcional del ABM con sus permisos institucionales.
- **US2 (P1)**: Después de US1; reutiliza la institución y permite a tesorería actualizar el estado.
- **US3 (P2)**: Después de US1 y US2 según el orden de fases solicitado; agrega el acceso para las áreas, filtros completos y paginación.

### Parallel Opportunities

- **US1**: T003, T004 y T005 verifican capas distintas y pueden prepararse en paralelo; T006 y T009 editan archivos separados una vez acordado el contrato.
- **US2**: T010, T011 y T012 son pruebas en archivos/capas distintos y pueden prepararse en paralelo; T015 puede implementarse en paralelo con T013/T014 al tocar configuración de seguridad.
- **US3**: T017, T018 y T019 pueden prepararse en paralelo; T020, T021 y T022 requieren que las pruebas estén definidas, y los cambios de consulta y OpenAPI pueden trabajarse en archivos separados.
- Las historias no se paralelizan en este plan porque el usuario solicitó el orden de fases indicado y comparten el recurso de institución.

## Parallel Example: User Story 1

```text
Preparar en paralelo:
- T003: pruebas unitarias de reglas de ABM
- T004: pruebas MVC/JPA de endpoints y preservación de remitos
- T005: prueba del contrato OpenAPI de ABM
```

## Implementation Strategy

### MVP First

1. Validar y aplicar la migración de base de datos (Phase 2).
2. Completar US1 para que personal institucional administre instituciones.
3. Detenerse y validar el ABM con sus pruebas unitarias, de integración y de contrato.

### Entrega incremental

1. Base de datos lista y validada en H2.
2. US1: ABM y baja lógica, manteniendo remitos e historial.
3. US2: estado de pago con permisos exclusivos de tesorería y sin bloqueo de remitos por deuda.
4. US3: consultas por área, filtros combinados y paginación.
5. Ejecutar validación focalizada y revisar el contrato OpenAPI al finalizar.
