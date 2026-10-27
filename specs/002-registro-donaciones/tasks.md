# Tasks: Registro de donaciones y comprobantes de recepción

**Input**: Design documents from `specs/002-registro-donaciones/` (`plan.md`,
`spec.md`, `research.md`, `data-model.md`, `contracts/openapi.yaml`).

**Prerequisites**: Java 21, Spring Boot, Flyway, JUnit 5, Mockito, H2 y Maven Wrapper
ya están configurados en el proyecto.

**Tests**: Se incluyen pruebas porque la constitución y el plan requieren escribirlas
antes o junto con cada regla de negocio y endpoint.

**Organization**: Las fases respetan la secuencia del equipo: V7 y Factura, Donante y
catálogo, Donación con Stock y comprobante en una transacción, y finalmente consultas
y permisos. Las etiquetas [US1]–[US3] conservan su correspondencia con `spec.md`.

## Phase 1: Setup (pruebas de compatibilidad)

**Purpose**: Preparar pruebas H2 que protejan la migración aditiva y los datos de
Factura existentes antes de modificar el esquema.

- [X] T001 [P] Agregar prueba de integración Flyway en `src/test/java/com/group6/BancoAlimentos/factura/migracion/MigracionV7CompatibilidadTest.java` que aplique V1–V7 en H2 limpio y compruebe que una factura V6 conserva institución, número, fecha, estado e importe, que admite institución nula para recibos nuevos, y que existen referencias/auditoría para donante y donación.
- [X] T002 [P] Agregar prueba de persistencia del comprobante en `src/test/java/com/group6/BancoAlimentos/factura/service/FacturaDonacionServiceTest.java` que compruebe asociación única a Donante/Donación, ausencia de importe económico, estado fijo `EMITIDA`, conservación de las facturas legadas y uso del ID autoincremental como numeración visible.

---

## Phase 2: Fundacional - Migración V7 y Factura

**Purpose**: Extender el esquema y el modelo de comprobantes sin transformar ni
perder datos históricos. Esta fase bloquea Donante y las donaciones.

- [X] T003 Crear `src/main/resources/db/migration/V7__donantes_donaciones_y_comprobantes.sql` con las tablas `donantes`, `donaciones` e `items_donacion`, sus claves foráneas e índices, sus tablas Envers, `productos.tipo_alimento` y `productos_aud.tipo_alimento`, y las columnas opcionales `facturas.donante_id`/`donacion_id` con FK e índice único por donación; permitir institución nula y ausencia de importe para comprobantes nuevos sin modificar valores de facturas anteriores. Incluir el seed de leche, harina y galletitas con tipo de alimento y una unidad base por producto, sin inventar taxonomías adicionales ni conversiones.
- [X] T004 Extender `src/main/java/com/group6/BancoAlimentos/factura/modelo/Factura.java`, `src/main/java/com/group6/BancoAlimentos/factura/dto/FacturaRespuesta.java` y `src/main/java/com/group6/BancoAlimentos/factura/mapper/FacturaMapper.java` para representar institución opcional, Donante/Donación opcionales para filas legadas, importe nulo para recibos y los datos asociados al comprobante sin romper la lectura existente.
- [X] T005 Crear `src/main/java/com/group6/BancoAlimentos/factura/service/FacturaService.java` para emitir comprobantes únicamente desde servicios internos: preservar los números legados, usar el ID autoincremental de Factura como número visible del nuevo comprobante, fijar estado `EMITIDA`, no calcular ni asignar importe económico y evitar endpoints de edición o anulación.

**Checkpoint**: V1–V7 aplica en H2 y una factura legada conserva sus valores; se puede
persistir un comprobante nuevo sin institución ni importe y con exactamente una
asociación a una donación.

---

## Phase 3: User Story 2 - Administrar donantes y productos (Priority: P2)

**Goal**: Mantener donantes y catálogo de productos para identificar de forma
consistente el origen y los artículos de futuras recepciones.

**Independent Test**: Crear y modificar un donante y un producto; desactivar el
donante conservando sus datos e historial; confirmar que los datos requeridos quedan
persistidos y que la baja impide su uso en nuevas donaciones.

### Tests for User Story 2

- [X] T006 [P] [US2] Agregar pruebas unitarias en `src/test/java/com/group6/BancoAlimentos/donante/service/DonanteServiceTest.java` para nombre obligatorio, datos de contacto opcionales, alta, actualización, baja lógica auditada y rechazo de reactivación/uso incompatible según las reglas existentes.
- [X] T007 [P] [US2] Agregar pruebas unitarias en `src/test/java/com/group6/BancoAlimentos/producto/service/ProductoDonacionServiceTest.java` para tipo obligatorio en productos nuevos, productos históricos sin clasificar, alta manual, actualización y desactivación lógica sin borrar producto con stock o historial.

### Implementation for User Story 2

- [X] T008 [US2] Crear la feature Donante en `src/main/java/com/group6/BancoAlimentos/donante/modelo/Donante.java`, `repository/DonanteRepository.java`, `service/DonanteService.java`, `dto/DonanteSolicitud.java`, `dto/DonanteRespuesta.java`, `mapper/DonanteMapper.java` y `controller/DonanteController.java`; auditar con Envers, exigir nombre no vacío, permitir contacto opcional y exponer alta, modificación y baja lógica. Dejar las rutas de consulta individual y listados para la fase de consultas.
- [X] T009 [US2] Extender `src/main/java/com/group6/BancoAlimentos/producto/modelo/Producto.java` y `src/main/java/com/group6/BancoAlimentos/producto/repository/ProductoRepository.java`, y agregar `ProductoSolicitud.java`, `ProductoMapper.java` y `ProductoService.java` bajo `src/main/java/com/group6/BancoAlimentos/producto/`; exigir tipo de alimento en altas nuevas, permitir que productos históricos sigan sin clasificar hasta su clasificación manual y soportar alta/actualización/baja lógica sin borrar productos referenciados. Mantener el nombre, la unidad y las consultas existentes.

**Checkpoint**: Donantes y productos se pueden crear, actualizar y desactivar
lógicamente; la baja conserva IDs, auditoría e historial. El listado y los filtros se
completan en la fase final.

---

## Phase 4: User Story 1 - Confirmar Donación con Stock y comprobante atómicos (Priority: P1)

**Goal**: Confirmar inmediatamente una donación completa, persistir sus líneas
inmutables, sumar las cantidades a Stock por producto-vencimiento y emitir exactamente
un comprobante dentro de una única transacción.

**Independent Test**: Confirmar una donación válida con varios productos y
vencimientos; verificar líneas y snapshots, cantidades exactas en Stock y comprobante
asociado. Probar líneas inválidas y fallos en Stock o Factura y verificar rollback
completo. Dos líneas del mismo producto y vencimiento permanecen en el detalle y se
suman al mismo lote de Stock.

### Tests for User Story 1

- [X] T010 [P] [US1] Agregar pruebas de reglas de líneas en `src/test/java/com/group6/BancoAlimentos/itemdonacion/service/ItemDonacionServiceTest.java` para al menos una línea, cantidad positiva representable con tres decimales, vencimiento posterior a la fecha de recepción, producto activo y snapshots obligatorios de nombre, tipo y unidad.
- [X] T011 [P] [US1] Agregar pruebas de ingreso de lotes en `src/test/java/com/group6/BancoAlimentos/stock/service/IngresoStockServiceTest.java` para sumar al par producto-vencimiento único, mantener distintos vencimientos separados, sumar líneas repetidas en el mismo lote, preservar estado/precisión y rechazar cantidades no positivas, exceso de escala o vencimiento inválido.
- [X] T012 [P] [US1] Agregar pruebas unitarias de coordinación en `src/test/java/com/group6/BancoAlimentos/donacion/service/DonacionServiceTest.java` para donante/productos activos, líneas inválidas, snapshots inmutables, producto manual, unidad base sin conversiones, estado `EMITIDA`, numeración por ID y fallos propagados de Stock/Factura.
- [X] T013 [US1] Agregar prueba H2 en `src/test/java/com/group6/BancoAlimentos/donacion/service/DonacionServiceIntegracionTest.java` que compruebe commit conjunto de Donación, ítems, Stock, auditorías y Factura, y rollback total ante error al guardar una línea, incrementar Stock o emitir el comprobante.

### Implementation for User Story 1

- [X] T014 [US1] Crear `src/main/java/com/group6/BancoAlimentos/donacion/modelo/Donacion.java`, `repository/DonacionRepository.java`, `service/DonacionService.java`, DTOs y mapper bajo `src/main/java/com/group6/BancoAlimentos/donacion/`; validar donante activo y fecha de recepción, y dejar `DonacionService.confirmar` como única operación transaccional coordinadora, sin acceso a repositorios de otras features ni captura de fallos como éxito.
- [X] T015 [US1] Crear `src/main/java/com/group6/BancoAlimentos/itemdonacion/modelo/ItemDonacion.java`, `repository/ItemDonacionRepository.java`, `service/ItemDonacionService.java`, DTOs y mapper bajo `src/main/java/com/group6/BancoAlimentos/itemdonacion/`; guardar cada línea recibida con snapshots de nombre, tipo y unidad, vínculo a producto y donación, cantidad positiva de hasta tres decimales y vencimiento futuro, sin operaciones de edición. Dejar el controller de consulta para la fase final.
- [X] T016 [US1] Agregar `src/main/java/com/group6/BancoAlimentos/stock/service/StockService.java` y extender `src/main/java/com/group6/BancoAlimentos/stock/repository/StockRepository.java` para resolver o crear mediante el servicio de Producto el lote único por producto-vencimiento, sumar la cantidad recibida y conservar clave única, estado, auditoría y reglas actuales; serializar altas concurrentes cuando corresponda y no introducir conversiones de unidades.
- [X] T017 [US1] Completar `src/main/java/com/group6/BancoAlimentos/donacion/service/DonacionService.java` para validar todas las líneas antes de confirmar, resolver productos de catálogo o manuales sin mutar fichas/snapshots históricos, guardar Donación e ítems, invocar `StockService` y `FacturaService` en la misma transacción y propagar excepciones para que Spring revierta todos los cambios.
- [X] T018 [US1] Implementar el endpoint `POST /api/donaciones` en `src/main/java/com/group6/BancoAlimentos/donacion/controller/DonacionController.java` con DTOs validados por Bean Validation y respuesta de donación confirmada con líneas y comprobante; no añadir borradores, edición o anulación de donaciones confirmadas.

**Checkpoint**: El flujo de confirmación cumple SC-001, SC-002 y SC-003; remitos y
reglas de vencimiento existentes no se alteran.

---

## Phase 5: User Story 3 - Consultas, filtros y permisos (Priority: P3)

**Goal**: Exponer los listados paginados y filtros acordados, restringiendo todas las
operaciones de esta funcionalidad al rol `user_deposito`.

**Independent Test**: Cargar donaciones de varios donantes, fechas y productos; aplicar
filtros y comprobar coincidencias exactas y paginación. Repetir lectura y escritura
como `user_tesoreria` y `user_institucional`; cada ruta de esta feature debe responder
403 y no persistir cambios.

### Tests for User Story 3

- [X] T019 [P] [US3] Agregar pruebas de filtros/paginación en `src/test/java/com/group6/BancoAlimentos/donacion/service/ConsultaDonacionServiceTest.java` y `src/test/java/com/group6/BancoAlimentos/factura/service/ConsultaFacturaServiceTest.java` para donante, intervalo de fechas, producto, vencimiento, estado y parámetros de página/tamaño, incluyendo asociación y detalle inmutable del comprobante.
- [X] T020 [P] [US3] Extender `src/test/java/com/group6/BancoAlimentos/producto/service/ConsultaProductoServiceTest.java`, `src/test/java/com/group6/BancoAlimentos/stock/service/ConsultaStockServiceTest.java` y agregar `src/test/java/com/group6/BancoAlimentos/donante/service/ConsultaDonanteServiceTest.java` para filtros por nombre, tipo, unidad, producto, fecha de vencimiento y estado, verificando resultados paginados.
- [X] T021 [P] [US3] Extender `src/test/java/com/group6/BancoAlimentos/Seguridad/MatrizPermisosConsultasTest.java` y `src/test/java/com/group6/BancoAlimentos/Seguridad/AutorizacionEndpointTest.java` con lecturas y escrituras de `/api/donantes`, `/api/donaciones`, `/api/donaciones/{id}/items`, `/api/productos`, `/api/stock` y `/api/facturas`; comprobar que solo `user_deposito` accede y que los 403 no producen escrituras.
- [X] T022 [P] [US3] Agregar prueba de contrato en `src/test/java/com/group6/BancoAlimentos/Seguridad/ContratoDonacionesOpenApiTest.java` que valide que las operaciones implementadas coinciden con `specs/002-registro-donaciones/contracts/openapi.yaml`, declaran `x-roles: [user_deposito]`, describen paginación y no exponen escritura de Factura.

### Implementation for User Story 3

- [X] T023 [US3] Implementar consultas paginadas y filtros mediante `DonanteRepository.java`/`ConsultaDonanteService.java`, `DonacionRepository.java`/`ConsultaDonacionService.java`, `ProductoRepository.java`/`ConsultaProductoService.java`, `StockRepository.java`/`ConsultaStockService.java` y `FacturaRepository.java`/`ConsultaFacturaService.java` en sus respectivos paquetes; admitir los parámetros del contrato y usar `Pageable` sin cambiar consultas existentes.
- [X] T024 [US3] Completar `DonanteController.java`, `DonacionController.java`, `ItemDonacionController.java`, `ProductoController.java`, `StockController.java` y `FacturaController.java` con los recursos de consulta del contrato (`GET` individual/listados, filtros y líneas anidadas); devolver DTOs paginados donde corresponda y mantener Factura en solo lectura.
- [X] T025 [US3] Actualizar `src/main/java/com/group6/BancoAlimentos/Security/ConfiguracionSeguridad.java` para declarar explícitamente `user_deposito` en cada método nuevo de donantes, donaciones, líneas, productos, stock y facturas, preservando las autorizaciones existentes de remitos, instituciones y otros recursos.
- [X] T026 [US3] Integrar `specs/002-registro-donaciones/contracts/openapi.yaml` en `src/main/resources/openapi.yaml` y documentar rutas, DTOs, filtros, paginación, errores y roles; reflejar comprobantes sin importe, estado fijo `EMITIDA`, numeración por ID y ausencia de operaciones de edición/anulación.

**Checkpoint**: Las consultas y permisos satisfacen SC-004; el contrato integrado
coincide con las rutas y respuestas reales.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verificar la entrega completa y proteger los flujos existentes.

- [X] T027 Ejecutar `.\mvnw.cmd clean verify` según `specs/002-registro-donaciones/quickstart.md`; confirmar V1–V7 en H2 limpio, pruebas de atomicidad/auditoría/permisos/contrato y regresión de remitos y vencimientos, y corregir cualquier fallo en los archivos afectados antes de cerrar la feature. Validación completada: 116 tests, 0 fallos, 0 errores y 1 omitido; BUILD SUCCESS.
- [ ] T028 Validar V7 con Flyway en MySQL real sobre una base limpia y una copia con facturas legadas, y ejecutar las pruebas condicionadas por `MYSQL_TEST_URL`; pendiente hasta disponer de una base MySQL de pruebas. No se ejecutó Docker.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: T001 y T002 son pruebas independientes que preceden a la
  migración y a la implementación de Factura.
- **Phase 2 (V7 y Factura)**: depende de las pruebas de Phase 1; debe completarse
  antes de Donante y de cualquier flujo de donación.
- **Phase 3 (US2: Donante y catálogo)**: depende de V7 y Factura; ambos servicios y
  el catálogo deben existir antes de confirmar donaciones.
- **Phase 4 (US1: Donación)**: depende de Phase 3 y coordina sus servicios con Stock y
  Factura en una sola transacción.
- **Phase 5 (US3: consultas y permisos)**: depende de los modelos y operaciones de
  Phase 2–4.
- **Phase 6 (Polish)**: depende de todas las fases anteriores.

### User Story Dependencies

- **US2 (P2)** se implementa antes que US1 porque Donante y Producto son prerequisitos
  de la recepción, aunque `spec.md` ordene las historias por prioridad funcional.
- **US1 (P1)** depende de US2 para resolver donantes y productos; su flujo confirmado
  no depende de la fase de listados.
- **US3 (P3)** depende de los datos y recursos de US1/US2 para filtrar y autorizar las
  consultas.

### Parallel Opportunities

- T001 y T002 pueden prepararse en paralelo, en archivos de prueba distintos.
- En Phase 3, T006 y T007 son pruebas independientes; después, el trabajo de Donante y
  Producto puede avanzar en paralelo en paquetes distintos.
- En Phase 4, T010, T011, T012 y T013 son pruebas por responsabilidad y pueden
  prepararse en paralelo; la integración requiere los servicios de Donante, Producto,
  Stock y Factura.
- En Phase 5, T019–T022 son tareas de prueba separadas; las consultas de features
  distintas pueden implementarse en paralelo, pero la configuración de seguridad y el
  contrato deben reflejar las rutas finales.
- No paralelizar dos tareas que alteren el mismo archivo de producción.

### Implementation Strategy

1. Validar primero la migración aditiva y la compatibilidad de Factura.
2. Completar Donante y catálogo antes de recibir mercadería.
3. Entregar la recepción como unidad indivisible: validación, snapshots, Stock y
   comprobante bajo la misma transacción.
4. Añadir después consultas, paginación, filtros, permisos y contrato.
5. Ejecutar validación completa y regresión antes de considerar terminada la feature.

**Suggested MVP**: Phase 2 y Phase 3 habilitan los prerequisitos; el primer incremento
de valor funcional es US1 (Phase 4), con un comprobante por donación y Stock actualizado
atómicamente. No es MVP funcional por sí solo si faltan las consultas y permisos de
Phase 5 requeridos para la entrega.

**Format validation**: Todas las tareas usan checkbox, ID secuencial, etiqueta [P]
solo cuando son paralelizables, etiqueta [US1]/[US2]/[US3] en fases de historias y
rutas exactas de archivos.
