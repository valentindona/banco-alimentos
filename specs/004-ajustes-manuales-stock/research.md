# Investigación: Ajustes manuales de stock

## Decisión: un solo límite transaccional en el service

- **Decisión**: ejecutar el caso de uso completo bajo `@Transactional`: cargar/bloquear el lote con `PESSIMISTIC_WRITE`, validar el saldo, calcular y asignar la nueva cantidad, e insertar el registro de ajuste. Las excepciones de regla de negocio deben propagarse para que Spring revierta ambas escrituras.
- **Rationale**: `StockRepository` ya define una consulta de bloqueo pesimista por ID ordenada (`buscarTodosPorIdBloqueadosOrdenados`) y los servicios que modifican stock ya operan con transacciones. El bloqueo serializa operaciones concurrentes sobre la fila y asegura que la cantidad anterior/nueva corresponda a la versión vigente.
- **Alternatives considered**: leer y guardar sin bloqueo (permite lecturas obsoletas); separar el registro o stock en otra transacción (rompe atomicidad); reemplazar `StockService` (contradice el alcance y duplica responsabilidad). No se requiere modificar los servicios existentes.
- **Fuentes**: [Spring Data JPA — Transactionality](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html), [Spring Data JPA — Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html), [Jakarta Persistence — LockModeType](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/LockModeType.html).

## Decisión: registro de negocio dedicado más Envers

- **Decisión**: persistir una entidad `Ajuste` append-only con autor, fecha/hora, referencias e instantánea del lote, tipo, saldo anterior/nuevo y motivo. Anotar la entidad con `@Audited` y mantener su tabla de Envers. No exponer actualizaciones ni eliminaciones en service, repository o API.
- **Rationale**: el requisito exige un historial consultable por campos del evento de dominio. Envers aporta revisiones del estado persistido, pero no sustituye el registro de negocio con motivo y cantidades semánticas. La entidad de stock ya está auditada.
- **Alternatives considered**: confiar solo en Envers (no ofrece el contrato de consulta e inmutabilidad del evento solicitado); modelar todo stock como ledger de eventos (cambio arquitectónico más amplio que excede esta feature); añadir triggers de DB (no están en el patrón vigente y no se requieren para evitar mutaciones desde la aplicación).
- **Fuente**: [Hibernate ORM User Guide — Envers](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#envers).

## Decisión: reutilizar el identificador de stock para bloquear el lote

- **Decisión**: el comando de creación recibe `stockId`. El service carga la fila con el método pesimistamente bloqueado existente y toma de ella `productoId` y `fechaVencimiento` para completar el snapshot del ajuste.
- **Rationale**: las respuestas de stock ya exponen el ID del lote, y `Stock` permite fecha de vencimiento nula. El identificador de fila evita ambigüedad y reutiliza el bloqueo, sin crear una segunda forma de localizar o modificar stock.
- **Alternatives considered**: pedir producto+vencimiento y añadir otra consulta de bloqueo; esa combinación requiere tratar el vencimiento nulo y podría duplicar lógica de acceso. Crear lotes nuevos se excluye expresamente.

## Decisión: remitos en borrador no reservan stock

- **Decisión**: permitir ajustes de stock referenciado desde remitos `BORRADOR`; no sincronizar ni modificar remitos al registrar ajustes. Al confirmar, dejar que la lógica existente vuelva a bloquear y validar todos los lotes y cantidades; una insuficiencia produce `ReglaNegocioException` y conserva el borrador y los saldos.
- **Rationale**: `ConfirmacionRemitoService.confirmar` ya valida disponibilidad bajo bloqueo antes de descontar, dentro de una transacción. Los borradores no descuentan stock al agregarse detalles.
- **Alternatives considered**: reservar saldo al crear/agregar detalles (cambiaría semántica y múltiples servicios); impedir ajustes mientras un lote esté en un borrador (contradice el requisito de permitirlos).

## Decisión: catálogo HTTP restringido a depósito y paginado

- **Decisión**: feature `ajuste`; `POST /api/ajustes` crea ajustes; `GET /api/ajustes` lista con los filtros de la feature y paginación existente (`pagina`, `tamano`, máximo 100). La autorización se declara en `ConfiguracionSeguridad` para ambos métodos y no se crean rutas PUT/DELETE.
- **Rationale**: sigue el estilo de `/api/stock`, `PaginaRespuesta`, la configuración central de Spring Security y los nombres de dominio en español. Los filtros se combinan con AND.
- **Alternatives considered**: un recurso editable o genérico de movimientos (no refleja la semántica inmutable y acotada solicitada); una respuesta paginada nueva (duplicaría `PaginaRespuesta`).
- **Fuentes de patrón**: `stock/controller/StockController`, `Common/respuesta/Paginacion`, `Common/respuesta/PaginaRespuesta`, `Security/ConfiguracionSeguridad`.

## Decisiones de validación y persistencia observadas en el repositorio

- `Stock.cantidadDisponible` es `DECIMAL(12,3)` y la migración V3 ya impide valores negativos; se reutilizan esa precisión, la escala y `ReglaNegocioException`.
- La tabla `stock` relaciona el producto y la migración V4 relaciona detalles con stock. La migración nueva es V9, posterior a V8, con las tablas `ajustes_stock` y `ajustes_stock_aud`, una FK a stock/producto/usuario y campos de auditoría Envers.
- La fecha/hora y el usuario se extraen del servidor y `UsuarioAutenticado`; nunca se aceptan como atributos del request.
- El perfil de pruebas ya ejecuta Flyway sobre H2 en modo MySQL y valida el esquema con Hibernate. No se necesita dependencia adicional.

## Supuestos que siguen abiertos

- No existe un SLA o volumen de datos acordado. Para el plan se adopta el tamaño máximo de página existente y se propone indexar las columnas de filtro más útiles; los índices concretos se afinan al implementar.
- El usuario se filtra por ID, y un rango de fechas inclusivo se evalúa en UTC porque el sistema guarda `Instant` y no define zona comercial.
- El request expresa `cantidad` como valor objetivo para `CORRECCION` y como cantidad a descontar para `BAJA`; para ambas operaciones se exige número positivo salvo corrección a cero. La precisión máxima es tres decimales, conforme al stock.
- La longitud máxima del motivo y la política para ajustes de stock de productos inactivos no están establecidas por la especificación. Se deben validar con la implementación/convenciones existentes antes de fijar restricciones adicionales. Mientras tanto, no son criterios de negocio asumidos.
- La inmutabilidad no añade protección frente a escrituras SQL externas: no hay evidencia de que el proyecto gestione permisos de escritura granulares o triggers append-only.
