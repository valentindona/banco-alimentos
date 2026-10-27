---
description: "Tareas de implementación para ajustes manuales de stock"
---

# Tasks: Ajustes manuales de stock

**Input**: Documentos de diseño en `specs/004-ajustes-manuales-stock/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/ajustes-stock.openapi.yaml`, `quickstart.md`

**Organización**: Las fases respetan el orden solicitado: migración y entidad; bajas; correcciones; consultas, filtros y permisos. Los tests aparecen antes de la implementación que verifican. Las etiquetas `[US1]` y `[US2]` corresponden a las historias de `spec.md`.

## Phase 1: Setup

**Propósito**: No se requieren cambios de inicialización ni dependencias. El plan confirma que Java 21, Spring Boot, Flyway, Envers, H2 y las herramientas de pruebas ya están configurados.

## Phase 2: Fundacional — Migración y entidad de ajuste

**Propósito**: Crear primero la persistencia del evento histórico; esta fase bloquea las operaciones y consultas de las historias.

**Criterio de prueba independiente**: Flyway aplica V9 en H2 y crea las tablas de negocio/auditoría, claves foráneas, restricciones e índices; la entidad persiste un evento inmutable y Envers registra su revisión.

- [X] T001 Agregar `AjusteMigracionTest` en `src/test/java/com/group6/BancoAlimentos/ajuste/migracion/AjusteMigracionTest.java` para verificar con Flyway/H2 que V9 crea `ajustes_stock`, `ajustes_stock_aud`, las FKs a stock/productos/usuarios y REVINFO, las restricciones de tipo/saldos y los índices de filtros.
- [X] T002 Crear `V9__crear_ajustes_stock.sql` en `src/main/resources/db/migration/V9__crear_ajustes_stock.sql`, siguiendo el patrón Envers existente, con columnas y snapshots del modelo, `DECIMAL(12,3)`, `motivo` sin límite máximo de negocio, FKs que impidan borrar referencias, restricciones de saldo/tipo e índices para producto, vencimiento, fecha/hora, usuario y tipo.
- [X] T003 Agregar `AjusteRepositoryTest` en `src/test/java/com/group6/BancoAlimentos/ajuste/repository/AjusteRepositoryTest.java` para comprobar persistencia de los snapshots, la inserción Envers y que la entidad de escritura no permita mutar ni borrar eventos.
- [X] T004 Crear `Ajuste`, `TipoAjuste` y el repositorio de solo inserción/consulta en `src/main/java/com/group6/BancoAlimentos/ajuste/modelo/Ajuste.java`, `src/main/java/com/group6/BancoAlimentos/ajuste/modelo/TipoAjuste.java` y `src/main/java/com/group6/BancoAlimentos/ajuste/repository/AjusteRepository.java`; usar `@Audited` y `@Immutable`, no exponer setters ni operaciones de actualización/borrado, y persistir autor, fecha UTC, stockId, producto/vencimiento snapshot, tipo, cantidades y motivo.

---

## Phase 3: User Story 1 — Registrar bajas (Priority: P1)

**Meta**: Permitir al rol de depósito descontar una cantidad válida de un lote existente y registrar el saldo anterior/nuevo y el motivo atómicamente.

**Prueba independiente**: Con un lote existente, una baja válida responde 201 y deja el saldo correcto y el evento consultable; entradas inválidas responden con error sin modificar saldo ni historial. Se permiten lotes vencidos y los remitos en borrador no reservan stock.

### Tests para bajas (antes de implementar)

- [X] T005 [P] [US1] Agregar pruebas de reglas de baja en `src/test/java/com/group6/BancoAlimentos/ajuste/service/AjusteServiceTest.java` y `src/test/java/com/group6/BancoAlimentos/ajuste/service/AjusteServiceConcurrenciaTest.java`: descuento exacto; cantidad cero, negativa o mayor al saldo; motivo nulo/vacío/solo espacios; saldo cero; rollback sin evento ni cambio de stock ante rechazo; y ajustes concurrentes sobre el mismo stock serializados sin saldo negativo y con cantidades anterior/nueva efectivas.
- [X] T006 [P] [US1] Agregar pruebas HTTP en `src/test/java/com/group6/BancoAlimentos/ajuste/controller/AjusteControllerIntegracionTest.java` para `POST /api/ajustes` con `stockId`, `tipo=BAJA`, `cantidad` y `motivo`; verificar 201 y respuesta snapshot, errores sin escrituras parciales, baja de lote vencido/bloqueado sin cambiar su estado, y ajuste permitido sobre un lote de remito BORRADOR con rechazo al confirmar si queda saldo insuficiente.

### Implementación de bajas

- [X] T007 [US1] Crear `AjusteSolicitud` y `AjusteRespuesta` en `src/main/java/com/group6/BancoAlimentos/ajuste/dto/AjusteSolicitud.java` y `src/main/java/com/group6/BancoAlimentos/ajuste/dto/AjusteRespuesta.java`; validar en el borde `stockId`, `tipo`, `cantidad` y motivo no vacío, sin aceptar usuario ni fecha desde el cuerpo.
- [X] T008 [US1] Crear `AjusteMapper` en `src/main/java/com/group6/BancoAlimentos/ajuste/mapper/AjusteMapper.java` para convertir entre la entidad inmutable y los DTOs, incluyendo cantidad anterior/nueva, autor y snapshot sin exponer entidades JPA.
- [X] T009 [US1] Implementar el caso BAJA en `src/main/java/com/group6/BancoAlimentos/ajuste/service/AjusteService.java`: usar una sola transacción, cargar `Stock` por `stockId` con el bloqueo pesimista existente antes de leer el saldo, obtener usuario de `UsuarioAutenticado` y hora UTC del servidor, validar cantidad positiva no superior al saldo, recortar/validar motivo, actualizar solo cantidad y guardar el evento; cualquier error debe revertir ambas escrituras.
- [X] T010 [US1] Crear `AjusteController` en `src/main/java/com/group6/BancoAlimentos/ajuste/controller/AjusteController.java` con `POST /api/ajustes`, `@Valid`, delegación al service y respuesta 201; no agregar rutas de edición o borrado.

---

## Phase 4: User Story 1 — Corregir cantidades (Priority: P1)

**Meta**: Permitir fijar la cantidad real del lote, incluso en cero, registrando también correcciones que dejan el mismo saldo.

**Prueba independiente**: Una corrección válida deja la cantidad objetivo y crea el evento correspondiente; una cantidad negativa falla sin cambios. Una corrección al saldo existente también deja registro.

### Tests para correcciones (antes de implementar)

- [X] T011 [P] [US1] Ampliar `src/test/java/com/group6/BancoAlimentos/ajuste/service/AjusteServiceTest.java` con pruebas de CORRECCION: cero y cantidades con hasta tres decimales válidas, valor negativo rechazado, mismo saldo aceptado con evento y cantidades anterior/nueva iguales.
- [X] T012 [P] [US1] Ampliar `src/test/java/com/group6/BancoAlimentos/ajuste/controller/AjusteControllerIntegracionTest.java` para verificar corrección HTTP válida, corrección a cero, registro del snapshot y rechazo de cantidad negativa sin modificar stock ni historial.

### Implementación de correcciones

- [X] T013 [US1] Completar la rama CORRECCION en `src/main/java/com/group6/BancoAlimentos/ajuste/service/AjusteService.java`: interpretar `cantidad` como saldo objetivo, aceptar cero y el mismo saldo, rechazar negativos y guardar cantidad anterior/nueva en la misma transacción y bajo el mismo bloqueo usado para BAJA.

---

## Phase 5: User Story 2 — Consultas, filtros y permisos (Priority: P2)

**Meta**: Permitir al depósito consultar el historial paginado con filtros AND y denegar las operaciones a otros roles.

**Prueba independiente**: Las consultas devuelven únicamente resultados que cumplen todos los filtros, ordenan por fechaHora e ID descendentes, respetan paginación y devuelven página vacía sin coincidencias. GET y POST responden 401 sin autenticación y 403 a roles no autorizados.

### Tests de consultas, filtros y permisos (antes de implementar)

- [X] T014 [P] [US2] Agregar `AjusteConsultaRepositoryTest` en `src/test/java/com/group6/BancoAlimentos/ajuste/repository/AjusteConsultaRepositoryTest.java` para verificar filtros combinables por producto, vencimiento exacto, fecha UTC inclusiva, usuario y tipo; vencimiento omitido sin filtro, rango invertido inválido, orden estable fechaHora/ID descendente y paginación.
- [X] T015 [P] [US2] Agregar pruebas de integración de listado en `src/test/java/com/group6/BancoAlimentos/ajuste/controller/AjusteControllerIntegracionTest.java` para filtros individuales/combinados de `GET /api/ajustes`, límites de página (máximo 100), orden, página vacía y rango de fechas invertido.
- [X] T016 [P] [US2] Agregar `AjustePermisosIntegracionTest` en `src/test/java/com/group6/BancoAlimentos/ajuste/controller/AjustePermisosIntegracionTest.java` para comprobar que GET y POST requieren autenticación (401), solo `user_deposito` tiene acceso (otros roles reciben 403) y una denegación no altera stock ni historial.
- [X] T017 [P] [US2] Agregar `ContratoAjustesOpenApiTest` en `src/test/java/com/group6/BancoAlimentos/Seguridad/ContratoAjustesOpenApiTest.java` para validar que el OpenAPI publicado documenta POST y GET, filtros, paginación y respuestas del contrato, y no declara PUT/DELETE.

### Implementación de consultas, filtros y permisos

- [X] T018 [US2] Implementar la consulta paginada y sus predicados en `src/main/java/com/group6/BancoAlimentos/ajuste/repository/AjusteRepository.java`; combinar solo los filtros informados con AND, tratar fechaDesde/fechaHasta como inclusivas sobre fechaHora UTC, ordenar fechaHora e ID descendentes y mantener solo métodos de lectura/inserción.
- [X] T019 [US2] Implementar el caso de uso de listado en `src/main/java/com/group6/BancoAlimentos/ajuste/service/AjusteService.java`, reutilizando `Paginacion` y `PaginaRespuesta`, convirtiendo resultados a DTOs y rechazando rangos invertidos como solicitud inválida.
- [X] T020 [US2] Agregar `GET /api/ajustes` en `src/main/java/com/group6/BancoAlimentos/ajuste/controller/AjusteController.java` con filtros opcionales `productoId`, `fechaVencimiento`, `fechaDesde`, `fechaHasta`, `usuarioId`, `tipo` y parámetros `pagina`/`tamano` existentes.
- [X] T021 [US2] Declarar explícitamente en `src/main/java/com/group6/BancoAlimentos/Security/ConfiguracionSeguridad.java` el acceso exclusivo de `user_deposito` a GET y POST `/api/ajustes`; no modificar políticas de otros endpoints.
- [X] T022 [US2] Incorporar POST, GET, esquemas, filtros, paginación, respuestas HTTP y seguridad del contrato en `src/main/resources/openapi.yaml`, siguiendo `specs/004-ajustes-manuales-stock/contracts/ajustes-stock.openapi.yaml` y sin rutas PUT/DELETE.

---

## Phase 6: Polish y validación transversal

**Propósito**: Mantener la guía ejecutable y comprobar la migración, reglas, endpoints, seguridad y filtros en conjunto.

- [X] T023 Actualizar los nombres de pruebas y escenarios de aceptación en `specs/004-ajustes-manuales-stock/quickstart.md` para reflejar las clases implementadas y cubrir BAJA, CORRECCION, concurrencia, rollback, consultas, permisos y remitos BORRADOR sin cambiar servicios existentes de stock/remitos.
- [X] T024 Ejecutar `.\mvnw.cmd -Dtest=AjusteServiceTest,AjusteServiceConcurrenciaTest,AjusteControllerIntegracionTest,AjusteMigracionTest,AjusteRepositoryTest,AjusteConsultaRepositoryTest,AjustePermisosIntegracionTest,ContratoAjustesOpenApiTest test` desde la raíz y resolver fallos vinculados a esta feature; los escenarios de validación están detallados en `specs/004-ajustes-manuales-stock/quickstart.md`.

---

## Dependencias y orden de ejecución

### Dependencias entre fases

- **Setup (Fase 1)**: No requiere cambios de proyecto ni dependencias nuevas.
- **Fundacional (Fase 2)**: V9 y entidad/repository; bloquea las operaciones y consultas posteriores.
- **Bajas (Fase 3)**: Depende de la persistencia; implementa primero el flujo de descuento solicitado.
- **Correcciones (Fase 4)**: Reutiliza el registro, el bloqueo y la transacción establecidos en la fase de bajas.
- **Consultas, filtros y permisos (Fase 5)**: Depende del historial persistido y de los DTOs; implementa GET y completa la autorización explícita.
- **Polish (Fase 6)**: Depende de las fases que se quieran entregar.

### Dependencias de historias

- **US1 (P1)**: Requiere la fase fundacional; sus bajas preceden a las correcciones conforme al orden pedido.
- **US2 (P2)**: Requiere que existan registros consultables. La autorización central de ambos endpoints se completa en su fase para mantener los permisos al final.

### Oportunidades de paralelismo

- T005 y T006 pueden escribirse en paralelo: prueban capas distintas de la operación BAJA.
- T011 y T012 pueden escribirse en paralelo: prueban la corrección en servicio e HTTP.
- T014, T015, T016 y T017 pueden prepararse en paralelo en archivos distintos; sus pruebas se ejecutan contra la implementación completa.
- Las implementaciones de una misma fase no son paralelas cuando modifican los mismos archivos (`AjusteService.java`, `AjusteController.java` o `AjusteRepository.java`).

## Estrategia de implementación

### MVP

1. Completar la migración V9 y la entidad append-only con Envers.
2. Completar y validar el flujo BAJA (US1, primera parte).
3. **Alcance MVP de negocio**: US1 completa al agregar correcciones; verificar ambas operaciones antes de habilitar consultas.

### Entrega incremental

1. Persistencia V9 y entidad histórica.
2. BAJA validada, con bloqueo pesimista y actualización/inserción atómicas.
3. CORRECCION validada sobre el saldo objetivo.
4. Consultas, filtros, paginación y permisos centralizados.
5. OpenAPI, quickstart y suite focalizada aprobados.

**Formato**: Todas las tareas usan checkbox, ID secuencial, etiqueta `[P]` solo cuando corresponde, etiqueta de historia en fases de historia y una ruta de archivo concreta.
