# Investigación: Ajustes manuales de stock

## Decisión: un solo límite transaccional en el service

- **Decisión**: ejecutar el caso de uso completo bajo `@Transactional`: cargar/bloquear el lote con `PESSIMISTIC_WRITE`, validar el saldo, calcular y asignar la nueva cantidad, e insertar el registro de ajuste. Las excepciones de regla de negocio deben propagarse para que Spring revierta ambas escrituras.
- **Rationale**: `StockRepository` ya define una consulta de bloqueo pesimista por ID ordenada (`buscarTodosPorIdBloqueadosOrdenados`) y los servicios que modifican stock ya operan con transacciones. El bloqueo serializa operaciones concurrentes sobre la fila y asegura que la cantidad anterior/nueva corresponda a la versión vigente.
- **Alternatives considered**: leer y guardar sin bloqueo (permite lecturas obsoletas); separar el registro o stock en otra transacción (rompe atomicidad); reemplazar `StockService` (contradice el alcance y duplica responsabilidad). No se requiere modificar los servicios existentes.
- **Fuentes**: [Spring Data JPA — Transactionality](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html), [Spring Data JPA — Locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html), [Jakarta Persistence — LockModeType](https://jakarta.ee/specifications/persistence/3.2/apidocs/jakarta.persistence/jakarta/persistence/LockModeType.html).

## Decisión: registro de negocio dedicado más Envers

- **Decisión**: persistir una entidad `Ajuste` con autor, fecha/hora, referencias e instantánea del lote, tipo, saldo anterior/nuevo y motivo. Envers audita inserciones, ediciones y eliminaciones. La edición y baja solo se permiten cuando no hay revisiones de stock posteriores al ajuste y el saldo actual coincide con su snapshot.
- **Rationale**: el requisito exige un historial consultable por campos del evento de dominio y permitir corregir ajustes sin sobrescribir movimientos posteriores. Envers conserva las versiones previas; el registro de negocio mantiene motivo y cantidades semánticas.
- **Alternatives considered**: confiar solo en Envers (no ofrece el contrato de consulta y motivo del evento); permitir cambios sin validar movimientos posteriores (puede sobrescribir el stock); modelar todo stock como ledger de eventos (cambio arquitectónico más amplio que excede esta feature); añadir triggers de DB (no están en el patrón vigente).
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

- **Decisión**: feature `ajuste`; `POST /api/ajustes` crea, `GET /api/ajustes` lista, `PUT /api/ajustes/{ajusteId}` edita y `DELETE /api/ajustes/{ajusteId}` revierte/baja un ajuste. Las mutaciones requieren que Envers no tenga revisiones de stock posteriores. La autorización se declara en `ConfiguracionSeguridad` para cada método.
- **Rationale**: sigue el estilo de `/api/stock`, `PaginaRespuesta`, la configuración central de Spring Security y los nombres de dominio en español. Los filtros se combinan con AND.
- **Alternatives considered**: permitir cambios sin revisar el historial de stock (podría sobrescribir movimientos posteriores); una respuesta paginada nueva (duplicaría `PaginaRespuesta`).
- **Fuentes de patrón**: `stock/controller/StockController`, `Common/respuesta/Paginacion`, `Common/respuesta/PaginaRespuesta`, `Security/ConfiguracionSeguridad`.

## Decisiones de validación y persistencia observadas en el repositorio

- `Stock.cantidadDisponible` es `DECIMAL(12,3)` y la migración V3 ya impide valores negativos; se reutilizan esa precisión, la escala y `ReglaNegocioException`.
- La tabla `stock` relaciona el producto y la migración V4 relaciona detalles con stock. V9 crea `ajustes_stock` y `ajustes_stock_aud`; V10 retira el constraint de tipo mal escrito sin reescribir V9. El enum y la validación del service limitan las escrituras de la API a los tipos admitidos.
- La fecha/hora y el usuario se extraen del servidor y `UsuarioAutenticado`; nunca se aceptan como atributos del request.
- El perfil de pruebas ya ejecuta Flyway sobre H2 en modo MySQL y valida el esquema con Hibernate. No se necesita dependencia adicional.

## Supuestos que siguen abiertos

- No existe un SLA o volumen de datos acordado. Para el plan se adopta el tamaño máximo de página existente y se propone indexar las columnas de filtro más útiles; los índices concretos se afinan al implementar.
- El usuario se filtra por ID, y un rango de fechas inclusivo se evalúa en UTC porque el sistema guarda `Instant` y no define zona comercial.
- El request expresa `cantidad` como valor objetivo para `CORRECCION` y como cantidad a descontar para `BAJA`; para ambas operaciones se exige número positivo salvo corrección a cero. La precisión máxima es tres decimales, conforme al stock.
- La longitud máxima del motivo y la política para ajustes de stock de productos inactivos no están establecidas por la especificación. Se deben validar con la implementación/convenciones existentes antes de fijar restricciones adicionales. Mientras tanto, no son criterios de negocio asumidos.
- Envers conserva las versiones de la aplicación, pero no protege frente a escrituras SQL externas: el proyecto no gestiona permisos granulares ni triggers.
