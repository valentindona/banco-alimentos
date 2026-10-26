# Tasks: Entrega de mercadería con remitos y control de vencimientos

**Input**: Design documents from `/specs/001-remitos-vencimientos/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Organization**: Setup, shared foundation, and base inventory entities precede user-story implementation. Tests are listed before the implementation tasks they verify, as requested.

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the minimum buildable Spring Boot project and its local runtime environment.

- [ ] T001 Create the Maven Wrapper project using Java 21 and Spring Boot Web MVC, Data JPA, Validation, Security, Envers, jjwt, Lombok, ModelMapper, Springdoc, Flyway, MySQL, H2, and Spring Boot Test dependencies in `pom.xml`, `mvnw`, and `mvnw.cmd`
- [ ] T002 [P] Ignore generated build output, IDE files, `.env`, and `application-local.properties` in `.gitignore`
- [ ] T003 [P] Define MySQL service, health check, persistent volume, and environment-only credentials in `docker-compose.yml`
- [ ] T004 Configure `application.yml`, `application-local.yml`, and `application-test.yml` for environment-driven MySQL/JWT settings and the H2 test profile in `src/main/resources/application.yml`, `src/main/resources/application-local.yml`, and `src/test/resources/application-test.yml`
- [ ] T005 Configure Flyway and the initial versioned schema migration for the application’s base schema in `src/main/resources/db/migration/V1__crear_esquema_base.sql`

**Checkpoint**: `.\mvnw.cmd -q -DskipTests package` builds the empty application and Docker Compose starts MySQL without any committed credentials.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared API errors, JWT authentication/authorization, and the minimal catalog, institution, and stock entities. This phase blocks all user stories.

### Shared errors

- [ ] T006 [P] Add unit tests for `ReglaNegocioException` status/code/message and the standard error DTO in `src/test/java/com/group6/BancoAlimentos/Common/excepcion/ReglaNegocioExceptionTest.java` and `src/test/java/com/group6/BancoAlimentos/Common/respuesta/ErrorRespuestaTest.java`
- [ ] T007 Implement `ReglaNegocioException` as HTTP 422 and the standard error response fields `fechaHora`, `estado`, `codigo`, `mensaje`, `ruta`, and `erroresCampo` in `src/main/java/com/group6/BancoAlimentos/Common/excepcion/ReglaNegocioException.java` and `src/main/java/com/group6/BancoAlimentos/Common/respuesta/ErrorRespuesta.java`
- [ ] T008 [P] Add MockMvc tests for bean-validation errors, HTTP 422 business errors, and sanitized HTTP 500 responses in `src/test/java/com/group6/BancoAlimentos/Common/handler/GlobalExceptionHandlerTest.java`
- [ ] T009 Implement `GlobalExceptionHandler` to map validation errors, `ReglaNegocioException`, authorization/resource errors, and unexpected errors to the standard error format without stack traces or secrets in `src/main/java/com/group6/BancoAlimentos/Common/handler/GlobalExceptionHandler.java`

### Authentication and authorization

- [ ] T010 [P] Add user entity and repository tests requiring unique `nombreUsuario`, nonempty `claveHash`, role `user_deposito`, `user_institucional`, or `user_tesoreria`, and `institucionId` for institutional users in `src/test/java/com/group6/BancoAlimentos/usuario/modelo/UsuarioRepositoryTest.java`
- [ ] T011 Implement `Usuario`, `RolUsuario`, and `UsuarioRepository`; enforce unique username, required password hash/role, institutional-user institution association, and never expose `claveHash` in `src/main/java/com/group6/BancoAlimentos/usuario/modelo/Usuario.java`, `src/main/java/com/group6/BancoAlimentos/usuario/modelo/RolUsuario.java`, and `src/main/java/com/group6/BancoAlimentos/usuario/repository/UsuarioRepository.java`
- [ ] T012 [P] Add service tests for valid credentials, invalid credentials, disabled accounts, and password hashing in `src/test/java/com/group6/BancoAlimentos/usuario/service/AutenticacionServiceTest.java`
- [ ] T013 Implement `AutenticacionService`, login DTOs, and token response; hash passwords with Spring Security `PasswordEncoder` and never return hashes in `src/main/java/com/group6/BancoAlimentos/usuario/service/AutenticacionService.java`, `src/main/java/com/group6/BancoAlimentos/usuario/dto/LoginSolicitud.java`, and `src/main/java/com/group6/BancoAlimentos/usuario/dto/TokenRespuesta.java`
- [ ] T014 [P] Add JWT unit tests for signature validation, expiration, malformed token, and role claims in `src/test/java/com/group6/BancoAlimentos/Security/TokenJwtServiceTest.java`
- [ ] T015 Implement `TokenJwtService` with an environment-supplied signing key of at least 256 bits, explicit algorithm/signature and expiry validation, and no known fallback secret in `src/main/java/com/group6/BancoAlimentos/Security/TokenJwtService.java`
- [ ] T016 Add MockMvc tests for public login, authenticated routes, explicit role denial, and allowed role access for each role in `src/test/java/com/group6/BancoAlimentos/Security/AutorizacionEndpointTest.java`
- [ ] T017 Implement stateless `SecurityFilterChain`, JWT filter, `/api/auth/login` controller, and protected provision-only user creation; explicitly configure route role policies in `src/main/java/com/group6/BancoAlimentos/Security/ConfiguracionSeguridad.java`, `src/main/java/com/group6/BancoAlimentos/Security/FiltroJwt.java`, `src/main/java/com/group6/BancoAlimentos/usuario/controller/AutenticacionController.java`, and `src/main/java/com/group6/BancoAlimentos/usuario/service/ProvisionamientoUsuariosService.java`
- [ ] T018 Add an integration test that the application fails startup in production configuration when the JWT signing key is absent or shorter than 256 bits in `src/test/java/com/group6/BancoAlimentos/Security/ConfiguracionJwtTest.java`
- [ ] T019 Validate JWT configuration at application startup and prohibit default/embedded credentials in `src/main/java/com/group6/BancoAlimentos/Config/PropiedadesJwt.java` and `src/main/java/com/group6/BancoAlimentos/Config/ValidacionConfiguracion.java`

### Base entities required before remitos

- [ ] T020 [P] Add persistence tests for Producto requiring `nombre` and `unidadMedida`, with optional `descripcion` and `activo` selection state in `src/test/java/com/group6/BancoAlimentos/producto/modelo/ProductoRepositoryTest.java`
- [ ] T021 [P] Add persistence tests for Institucion requiring `nombre` and `domicilio`, with `estadoPago` limited to `AL_DIA`, `EN_DEUDA`, or `PARTICULAR`, and `activa` state in `src/test/java/com/group6/BancoAlimentos/institucion/modelo/InstitucionRepositoryTest.java`
- [ ] T022 Implement Producto and Institucion entities/repositories with `nombre` and `unidadMedida` required, `descripcion` optional, `estadoPago` limited to `AL_DIA`, `EN_DEUDA`, or `PARTICULAR`, and required `nombre`/`domicilio`; allow `EN_DEUDA` as a valid destination in `src/main/java/com/group6/BancoAlimentos/producto/modelo/Producto.java`, `src/main/java/com/group6/BancoAlimentos/producto/repository/ProductoRepository.java`, `src/main/java/com/group6/BancoAlimentos/institucion/modelo/Institucion.java`, and `src/main/java/com/group6/BancoAlimentos/institucion/repository/InstitucionRepository.java`
- [ ] T023 [P] Add stock persistence tests for unique `(productoId, fechaVencimiento)`, nullable `fechaVencimiento`, nonnegative `cantidadDisponible`, and states `DISPONIBLE`, `VENCIDO`, and `SIN_VENCIMIENTO` in `src/test/java/com/group6/BancoAlimentos/stock/modelo/StockRepositoryTest.java`
- [ ] T024 Implement Stock entity/repository, reusable single null-expiration row per product, unique product/expiration key, nonnegative quantity constraint, and the three specified states in `src/main/java/com/group6/BancoAlimentos/stock/modelo/Stock.java` and `src/main/java/com/group6/BancoAlimentos/stock/repository/StockRepository.java`
- [ ] T025 Create Flyway migration constraints/indexes and development/test fixture data for Producto, Institucion, Stock, and Usuario; limit fixtures to local/test profiles in `src/main/resources/db/migration/V2__crear_catalogo_instituciones_stock_y_usuarios.sql`, `src/main/resources/db/migration/V3__restricciones_inventario.sql`, and `src/test/resources/data.sql`
- [ ] T026 Add repository tests for stock lookup by id and product/expiration pair, and institution/product lookup by id needed by remito validation in `src/test/java/com/group6/BancoAlimentos/stock/repository/StockFiltrosRepositoryTest.java`
- [ ] T027 Implement repository lookup methods for stock by id and product/expiration pair, institution by id, and product by id needed by remito validation in `src/main/java/com/group6/BancoAlimentos/stock/repository/StockRepository.java`, `src/main/java/com/group6/BancoAlimentos/institucion/repository/InstitucionRepository.java`, and `src/main/java/com/group6/BancoAlimentos/producto/repository/ProductoRepository.java`

**Checkpoint**: Exception handling, token authentication, all three roles, and the product/institution/stock persistence foundation work before remito tasks begin.

## Phase 3: User Story 1 - Registrar y confirmar una entrega segura (Priority: P1) 🎯 MVP

**Goal**: Deposit staff prepare a remito, explicitly select product/expiration stock, and atomically confirm valid deliveries.

**Independent Test**: Create a valid remito against available future-dated stock and confirm exact deductions. Expired stock, empty/duplicate lines, invalid institution, or insufficient stock reject without modifying any stock; an indebted institution remains eligible.

### Tests for User Story 1

- [ ] T028 [P] [US1] Add entity and audit tests for Remito states `BORRADOR` and `CONFIRMADO`, required institution/date, snapshots, nullable `fechaConfirmacion` while draft, creator user, and Envers history for Producto/Stock/Institucion/Remito in `src/test/java/com/group6/BancoAlimentos/remito/modelo/RemitoRepositoryTest.java` and `src/test/java/com/group6/BancoAlimentos/Config/AuditoriaNegocioTest.java`
- [ ] T029 [P] [US1] Add entity and Envers history tests for DetalleRemito positive `cantidad`, required stock/remito, and unique `(remitoId, stockId)` while allowing the same product at different expirations in `src/test/java/com/group6/BancoAlimentos/detalleremito/modelo/DetalleRemitoRepositoryTest.java`
- [ ] T030 [P] [US1] Add `RemitoService` unit tests for debt-allowed institution, missing/incomplete institution rejection, empty remito, expired-today/past stock, missing expiration, duplicate product-stock, and positive quantity in `src/test/java/com/group6/BancoAlimentos/remito/service/RemitoServiceTest.java`
- [ ] T031 [P] [US1] Add confirmation service tests proving all lines are revalidated, insufficient stock leaves every quantity unchanged, valid confirmation deducts exact quantities, and a second confirmation cannot deduct twice in `src/test/java/com/group6/BancoAlimentos/remito/service/ConfirmacionRemitoServiceTest.java`
- [ ] T032 [P] [US1] Add H2 MockMvc endpoint tests for create draft, add detail, confirm, DTO validation, HTTP 422 error shape, and denial for treasury/institutional roles in `src/test/java/com/group6/BancoAlimentos/remito/controller/RemitoControllerTest.java`
- [ ] T033 [P] [US1] Add MySQL integration test for simultaneous confirmations against one stock row and assert no negative balance or partial deduction in `src/test/java/com/group6/BancoAlimentos/remito/service/ConfirmacionRemitoConcurrenciaTest.java`

### Implementation for User Story 1

- [ ] T034 [US1] Implement Remito entity/repository with required `institucionId` and `fechaEntrega`, state `BORRADOR` or `CONFIRMADO`, `fechaConfirmacion` null while draft, required destination snapshots, and required `creadoPorUsuarioId` in `src/main/java/com/group6/BancoAlimentos/remito/modelo/Remito.java` and `src/main/java/com/group6/BancoAlimentos/remito/repository/RemitoRepository.java`
- [ ] T035 [US1] Implement DetalleRemito entity/repository with required `remitoId` and `stockId`, `cantidad` required and greater than zero, and unique `(remitoId, stockId)` in `src/main/java/com/group6/BancoAlimentos/detalleremito/modelo/DetalleRemito.java` and `src/main/java/com/group6/BancoAlimentos/detalleremito/repository/DetalleRemitoRepository.java`
- [ ] T036 [US1] Add Flyway migration for remito and detail tables, required foreign keys, snapshot fields, unique `(remito_id, stock_id)`, and audit tables in `src/main/resources/db/migration/V4__crear_remitos_detalles_y_auditoria.sql`
- [ ] T037 [US1] Implement request/response DTOs with Bean Validation and mappers for creating remitos, adding selected stock/quantity, and returning destination/product/expiration snapshots in `src/main/java/com/group6/BancoAlimentos/remito/dto/CrearRemitoSolicitud.java`, `src/main/java/com/group6/BancoAlimentos/remito/dto/RemitoRespuesta.java`, `src/main/java/com/group6/BancoAlimentos/detalleremito/dto/AgregarDetalleSolicitud.java`, `src/main/java/com/group6/BancoAlimentos/detalleremito/dto/DetalleRemitoRespuesta.java`, `src/main/java/com/group6/BancoAlimentos/remito/mapper/RemitoMapper.java`, and `src/main/java/com/group6/BancoAlimentos/detalleremito/mapper/DetalleRemitoMapper.java`
- [ ] T038 [US1] Implement `RemitoService` rules to require an existing active institution with required name/address, accept `EN_DEUDA`, require a nonempty future delivery date, reject nonpositive quantity, missing expiration, expired-today/past stock, and duplicate stock line via `ReglaNegocioException` in `src/main/java/com/group6/BancoAlimentos/remito/service/RemitoService.java`
- [ ] T039 [US1] Implement `ConfirmacionRemitoService` as one `@Transactional` operation: lock the remito, lock all requested Stock rows in ascending id order, revalidate every line and `fechaVencimiento > fechaNegocio`, then deduct all quantities and transition to `CONFIRMADO`; perform no draft reservations in `src/main/java/com/group6/BancoAlimentos/remito/service/ConfirmacionRemitoService.java` and `src/main/java/com/group6/BancoAlimentos/stock/repository/StockRepository.java`
- [ ] T040 [US1] Implement remito REST controller under `/api/remitos` for create draft, add detail, and confirm, apply `@Valid`, return DTOs, and restrict every write operation to `user_deposito` in `src/main/java/com/group6/BancoAlimentos/remito/controller/RemitoController.java` and `src/main/java/com/group6/BancoAlimentos/detalleremito/controller/DetalleRemitoController.java`
- [ ] T041 [US1] Add audit configuration for business entities `Producto`, `Stock`, `Institucion`, `Remito`, and `DetalleRemito`, excluding `Usuario` and `Notificacion` in `src/main/java/com/group6/BancoAlimentos/Config/ConfiguracionAuditoria.java` and the respective entity model files

**Checkpoint**: User Story 1 is testable end-to-end through `/api/remitos`, exact stock deductions are atomic, and every business rejection returns the standard HTTP 422 error shape.

## Phase 4: User Story 2 - Detectar stock próximo a vencer o vencido (Priority: P2)

**Goal**: Persist daily, idempotent on-screen alerts for warehouse users and make expired stock ineligible even before the scheduled job runs.

**Independent Test**: With a fixed business date, daily processing creates alerts for dates 1-3 days away and expired stock, not day 4; rerunning creates no duplicates, and non-deposit roles cannot read alerts.

### Tests for User Story 2

- [ ] T042 [P] [US2] Add Stock service tests for date today becoming `VENCIDO`, future stock inside three days remaining eligible but alertable, day-four exclusion, and missing expiration being `SIN_VENCIMIENTO` in `src/test/java/com/group6/BancoAlimentos/stock/service/VencimientoStockServiceTest.java`
- [ ] T043 [P] [US2] Add notification repository tests for one `POR_VENCER` and one `VENCIDO` event per stock, role audience `user_deposito`, and idempotent reprocessing in `src/test/java/com/group6/BancoAlimentos/notificacion/repository/NotificacionRepositoryTest.java`
- [ ] T044 [P] [US2] Add scheduler/service tests for overdue catch-up, 3-day inclusive boundary, day-four exclusion, positive quantity filter, repeated execution, and notification persistence in `src/test/java/com/group6/BancoAlimentos/notificacion/service/ProcesoDiarioVencimientosServiceTest.java`
- [ ] T045 [P] [US2] Add MockMvc tests for deposit-only paginated notification listing and 403 for treasury/institutional roles in `src/test/java/com/group6/BancoAlimentos/notificacion/controller/NotificacionControllerTest.java`

### Implementation for User Story 2

- [ ] T046 [US2] Implement `Notificacion` entity/repository with required `stockId`, `audiencia` equal to `user_deposito`, `tipo` limited to `POR_VENCER` or `VENCIDO`, required creation timestamp/message, and unique `(stockId, tipo)` in `src/main/java/com/group6/BancoAlimentos/notificacion/modelo/Notificacion.java` and `src/main/java/com/group6/BancoAlimentos/notificacion/repository/NotificacionRepository.java`
- [ ] T047 [US2] Add Flyway migration for persisted notifications with foreign key to Stock and unique `(stock_id, tipo)` idempotency constraint in `src/main/resources/db/migration/V5__crear_notificaciones.sql`
- [ ] T048 [US2] Implement `ProcesoDiarioVencimientosService` to mark all positive-quantity stock with `fechaVencimiento <= fechaNegocio` as `VENCIDO`, create missing `VENCIDO` alerts, and create `POR_VENCER` alerts only for dates from tomorrow through day three in one repeat-safe transaction in `src/main/java/com/group6/BancoAlimentos/notificacion/service/ProcesoDiarioVencimientosService.java` and `src/main/java/com/group6/BancoAlimentos/stock/service/VencimientoStockService.java`
- [ ] T049 [US2] Implement the daily configurable `@Scheduled` trigger at local midnight using `America/Argentina/Buenos_Aires` by default; keep the transactional rules in the service in `src/main/java/com/group6/BancoAlimentos/notificacion/service/ProgramadorVencimientos.java` and `src/main/java/com/group6/BancoAlimentos/Config/ConfiguracionProgramador.java`
- [ ] T050 [US2] Implement paginated notification read service/controller under `/api/notificaciones`, filterable by type, DTO-only, and explicitly restricted to `user_deposito` in `src/main/java/com/group6/BancoAlimentos/notificacion/service/ConsultaNotificacionService.java`, `src/main/java/com/group6/BancoAlimentos/notificacion/controller/NotificacionController.java`, and `src/main/java/com/group6/BancoAlimentos/notificacion/dto/NotificacionRespuesta.java`

**Checkpoint**: The scheduled operation is repeat-safe, alerts are visible only to deposit, and confirmation still blocks expired stock without waiting for the schedule.

## Phase 5: User Story 3 - Consultar y filtrar información relacionada (Priority: P3)

**Goal**: Provide paginated read-only views with strictly role-scoped access for remitos, products, stock, institutions, and minimal invoice projection.

**Independent Test**: Using each role, query each allowed list and filters; verify all disallowed role/list combinations return 403 and institutional remito results are restricted to the institution in the authenticated user record.

### Tests for User Story 3

- [ ] T051 [P] [US3] Add repository/service tests for remito filters by institution/date/state and pagination, including empty results and page bounds in `src/test/java/com/group6/BancoAlimentos/remito/service/ConsultaRemitoServiceTest.java`
- [ ] T052 [P] [US3] Add service/security tests proving institutional remito queries are always scoped to the institution attached to the authenticated user and ignore client-supplied institution ids in `src/test/java/com/group6/BancoAlimentos/remito/service/ConsultaRemitosInstitucionServiceTest.java`
- [ ] T053 [P] [US3] Add product, stock, and institution filter tests including product name/active state, stock product/expiration/state, and institution name/payment state in `src/test/java/com/group6/BancoAlimentos/producto/service/ConsultaProductoServiceTest.java`, `src/test/java/com/group6/BancoAlimentos/stock/service/ConsultaStockServiceTest.java`, and `src/test/java/com/group6/BancoAlimentos/institucion/service/ConsultaInstitucionServiceTest.java`
- [ ] T054 [P] [US3] Add invoice read-only tests for institution/date/state filters, pagination, deposit-only access, Envers history, and absence of create/update operations in `src/test/java/com/group6/BancoAlimentos/factura/service/ConsultaFacturaServiceTest.java` and `src/test/java/com/group6/BancoAlimentos/factura/controller/FacturaControllerTest.java`
- [ ] T055 [P] [US3] Add full role-matrix integration tests for remito/product/stock/institution/invoice endpoints, ensuring only depósito sees product/stock/invoice, tesorería sees institutions, and institucional sees own remitos in `src/test/java/com/group6/BancoAlimentos/Seguridad/MatrizPermisosConsultasTest.java`

### Implementation for User Story 3

- [ ] T056 [US3] Implement paginated Producto, Stock, and Institucion query services/controllers/DTOs using their tested filters; allow only `user_deposito` for products/stock and `user_deposito`/`user_tesoreria` for institutions in `src/main/java/com/group6/BancoAlimentos/producto/service/ConsultaProductoService.java`, `src/main/java/com/group6/BancoAlimentos/producto/controller/ProductoController.java`, `src/main/java/com/group6/BancoAlimentos/producto/dto/ProductoRespuesta.java`, `src/main/java/com/group6/BancoAlimentos/stock/service/ConsultaStockService.java`, `src/main/java/com/group6/BancoAlimentos/stock/controller/StockController.java`, `src/main/java/com/group6/BancoAlimentos/stock/dto/StockRespuesta.java`, `src/main/java/com/group6/BancoAlimentos/stock/mapper/StockMapper.java`, `src/main/java/com/group6/BancoAlimentos/institucion/service/ConsultaInstitucionService.java`, `src/main/java/com/group6/BancoAlimentos/institucion/controller/InstitucionController.java`, and `src/main/java/com/group6/BancoAlimentos/institucion/dto/InstitucionRespuesta.java`
- [ ] T057 [US3] Implement paginated remito query/controller under `/api/remitos` with filters by institution/date/state for `user_deposito` and `user_institucional`; scope institutional users to their own institution and return DTOs only in `src/main/java/com/group6/BancoAlimentos/remito/repository/RemitoRepository.java`, `src/main/java/com/group6/BancoAlimentos/remito/controller/ConsultaRemitoController.java`, and `src/main/java/com/group6/BancoAlimentos/remito/service/ConsultaRemitoService.java`
- [ ] T058 [US3] Implement read-only Factura projection/entity/repository with required fields `numero`, `institucionId`, `fecha`, `estado`, and `importe`; audit it with Envers and add no generation/payment/update operations in `src/main/java/com/group6/BancoAlimentos/factura/modelo/Factura.java`, `src/main/java/com/group6/BancoAlimentos/factura/repository/FacturaRepository.java`, and `src/main/resources/db/migration/V6__crear_proyeccion_consulta_facturas.sql`
- [ ] T059 [US3] Implement paginated read-only `/api/facturas` filters by institution/date/state, DTO and mapper; restrict all access to `user_deposito` in `src/main/java/com/group6/BancoAlimentos/factura/controller/FacturaController.java`, `src/main/java/com/group6/BancoAlimentos/factura/service/ConsultaFacturaService.java`, `src/main/java/com/group6/BancoAlimentos/factura/dto/FacturaRespuesta.java`, and `src/main/java/com/group6/BancoAlimentos/factura/mapper/FacturaMapper.java`
- [ ] T060 [US3] Update `src/main/resources/openapi.yaml` and Springdoc configuration to document every implemented `/api/...` endpoint, DTO, filter, pagination parameter, error response, and role policy using `specs/001-remitos-vencimientos/contracts/openapi.yaml` as the contract source

**Checkpoint**: All role-filtered read scenarios and pagination work independently of the notification scheduler and remito write flow.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate that the complete feature, build, deployment guide, and docs agree.

- [ ] T061 [P] Run the Envers integration regression suite for Producto, Stock, Institucion, Remito, DetalleRemito, and Factura, confirming user hashes and notifications remain unaudited in `src/test/java/com/group6/BancoAlimentos/Config/AuditoriaNegocioTest.java`
- [ ] T062 [P] Add a test-profile bootstrap fixture for the three roles and representative product/institution/stock/invoice data without production credentials in `src/test/resources/data.sql`
- [ ] T063 Validate all Flyway migrations against clean MySQL and H2 test schema, fixing compatibility in `src/main/resources/db/migration/`
- [ ] T064 Run the full suite using `.\mvnw.cmd clean verify`, correct failures, and record the passing command and expected outcomes in `specs/001-remitos-vencimientos/quickstart.md`
- [ ] T065 Validate the published Springdoc document against `specs/001-remitos-vencimientos/contracts/openapi.yaml` and correct contract drift in `src/main/resources/openapi.yaml` and `src/main/java/com/group6/BancoAlimentos/Config/ConfiguracionOpenApi.java`
- [ ] T066 Verify local startup and secret hygiene using `docker-compose.yml`, `.gitignore`, and `specs/001-remitos-vencimientos/quickstart.md`; confirm no secret or credential is tracked

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; complete the build, configuration, local database, and migration framework first.
- **Foundational (Phase 2)**: Depends on Setup and blocks all user stories; implement errors and authentication before base entities, then Producto/Institucion before Stock.
- **User Story 1 (Phase 3)**: Depends on the complete foundational phase; delivers the MVP remito write/confirmation journey.
- **User Story 2 (Phase 4)**: Depends on base Stock entities and shared security; can be developed after foundation and tested independently, with final expiry rejection integrated in the US1 confirmation service.
- **User Story 3 (Phase 5)**: Depends on base entities/security; remito list reads require the US1 Remito model, and notification-independent reads can proceed after foundation.
- **Polish (Phase 6)**: Depends on the stories selected for release; run after desired stories are integrated.

### User Story Dependencies

- **US1 (P1)**: Requires Foundation; no dependency on US2 or US3.
- **US2 (P2)**: Requires Foundation Stock; final stock expiry/notification controller work uses the shared Stock model and security.
- **US3 (P3)**: Requires Foundation; remito query endpoints use the US1 Remito entity. Product, stock, institution, and invoice queries can be developed independently.
- Complete US1 first for the smallest end-to-end delivery MVP; then US2 and US3 can be parallelized by separate contributors after their prerequisites exist.

### Within Each Phase

- Every test task must be implemented and shown failing before the corresponding production implementation task begins.
- Foundation dependencies are ordered by compile/runtime dependency: project and DB → shared error handling → auth/security → Producto/Institucion → Stock.
- In each user story, tests precede the entity/service/controller they validate; entities/repositories precede services; services precede controllers.
- Use Flyway migrations for all persistent entities/constraints; do not rely on `ddl-auto=update`.

### Parallel Opportunities

- Setup T002 and T003 can run in parallel with T001; T004 follows the selected dependencies in T001; T005 follows T004.
- Foundation T006/T008 and user-auth test tasks T010/T012/T014/T016 can be authored concurrently once the base test harness exists; corresponding implementations remain ordered after their tests.
- Product and Institution persistence tests T020/T021 can run in parallel; the Stock model test T023 can be authored in parallel but its implementation T024 follows Product.
- In US1, independent entity and service tests T028-T033 can be prepared in parallel; implementation starts only after the relevant failing tests exist. DTO mapping and repository work can be split after model decisions.
- In US2, scheduler, notification, and controller tests T042-T045 can be authored in parallel; implementation tasks split by notification storage, expiry service, scheduler, and endpoint.
- In US3, filter tests T051-T055 can be authored in parallel. Product/institution read endpoints and invoice read-only projection are separate work after foundation.

## Parallel Example: Foundation

```text
Contributor A: T006, T008 (shared exception/handler tests), then T007/T009
Contributor B: T010, T012, T014, T016 (auth test files), then T011/T013/T015/T017/T019
Contributor C: T020/T021 (Producto and Institucion tests), then T022; T023/T024 Stock follows T022
```

## Parallel Example: User Story 1

```text
Contributor A: T028/T029 model tests, then T034/T035 entities and repositories
Contributor B: T030/T031 service tests, then T038/T039 transactional business rules
Contributor C: T032/T033 API and MySQL concurrency tests, then T037/T041 DTOs and endpoints
```

## Parallel Example: User Story 2

```text
Contributor A: T042/T044 expiry/scheduler tests, then T048/T049 processing
Contributor B: T043 notification persistence tests, then T046/T047 notification model and migration
Contributor C: T045 notification endpoint test, then T050 notification read endpoint
```

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1 project setup.
2. Complete Phase 2 shared errors, minimal JWT/roles, and Producto/Institucion/Stock persistence.
3. Implement Phase 3 remito draft, selected detail, and atomic confirmation.
4. Run all T028-T041 applicable tests; validate exact stock deductions, no partial updates, and authorization.
5. Stop for review; do not start US2/US3 until the MVP passes.

### Incremental Delivery

1. Add US2 daily expiry marking, idempotent persisted alerts, and deposit-only notification query.
2. Add US3 paginated, role-scoped read endpoints and the minimum read-only invoice projection.
3. Complete Phase 6 audits, migration checks, API contract validation, and quickstart.

## Test-First Traceability

| Implementation scope | Test task(s) that precede it |
|---|---|
| Standard exception and response DTO | T006 → T007 |
| Global exception handler | T008 → T009 |
| User model/repository | T010 → T011 |
| Authentication service | T012 → T013 |
| JWT token service | T014 → T015 |
| Security filter chain/controllers | T016 → T017 |
| Producto and Institucion entities | T020, T021 → T022 |
| Stock entity/repository | T023 → T024 |
| Remito and DetalleRemito models | T028, T029 → T034, T035 |
| Remito business/confirmation services | T030, T031 → T038, T039 |
| Remito endpoints | T032 → T040 |
| Envers business-entity configuration | T028-T029 → T041 |
| Expiry and notification persistence/processing | T042-T044 → T046-T049 |
| Notification endpoint | T045 → T050 |
| Read filters/role policies/invoice consultation | T051-T055 → T056-T060 |
