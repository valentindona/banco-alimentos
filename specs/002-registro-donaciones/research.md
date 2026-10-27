# Research: Registro de donaciones y comprobantes de recepción

## Decisiones

### D1. Reutilizar la proyección Factura con vínculos nuevos y migración V7

- **Decisión**: Extender `Factura` y `facturas` con `donante_id` y `donacion_id`
  opcionales, con una restricción de unicidad para garantizar como máximo un
  comprobante por donación. Permitir `institucion_id` nulo para las nuevas recepciones.
  Conservar las columnas y valores de facturas existentes y actualizar la tabla de
  auditoría de Factura.
- **Rationale**: V6 define `institucion_id` obligatorio y FK a instituciones. El modelo
  Java también lo exige. La entidad no tiene vínculo a remito, y hoy solo existe una
  lectura paginada; por lo tanto, la asociación institucional no representa la nueva
  recepción a un donante. Una factura nueva debe apuntar a donante y donación sin
  inventar una institución.
- **Alternatives considered**:
  - Reemplazar Factura por una nueva entidad: rechazado porque rompería la proyección
    existente y duplicaría el concepto de comprobante.
  - Fabricar una institución para cada recibo o asociar el recibo a una institución
    ajena: rechazado porque falsearía el origen de la recepción.
  - Convertir las filas históricas a donaciones: rechazado porque no hay datos para
    reconstruir donantes o productos recibidos.
- **Compatibility**: Migration V7 conserva datos de institución, estado e importe en
  las filas existentes; los campos nuevos permanecen nulos para esas filas. La
  migración debe validarse en H2 y MySQL antes de cerrar su sintaxis.

### D2. Mantener el servicio de donación como coordinador transaccional

- **Decisión**: Una única operación de `DonacionService`, anotada transaccionalmente,
  valida y persiste donación e ítems, pide el ingreso a `StockService` y la emisión a
  `FacturaService`. Todos los servicios participantes usan la transacción existente,
  sin `REQUIRES_NEW`, llamadas HTTP ni transacciones separadas.
- **Rationale**: El flujo actual de confirmación de remitos ya usa un service
  transaccional, bloquea y valida todas las existencias antes del descuento, y propaga
  `ReglaNegocioException`. Adoptar ese patrón preserva atomicidad y separa los
  repositorios por feature.
- **Alternatives considered**:
  - Actualizar stock desde el controller o desde repositorios de otras features:
    rechazado por la constitución y por la separación vertical.
  - Crear la factura en un segundo paso: rechazado porque permite una recepción sin
    comprobante o un comprobante sin stock.
  - Añadir un nuevo coordinador distribuido: rechazado por complejidad innecesaria en
    una aplicación monolítica.

### D3. Incrementar el lote existente por producto y vencimiento

- **Decisión**: Resolver o crear la existencia usando el par producto-fecha existente
  y sumar la cantidad recibida; conservar las reglas de vencimiento, estado y precisión
  de Stock. Usar la clave única existente como invariante y serializar el ingreso por
  producto cuando se requiera resolver altas concurrentes del mismo lote.
- **Rationale**: Stock ya tiene `uq_stock_producto_vencimiento`, filtros por fecha y
  estado, y el flujo de remitos elige existencias por vencimiento. Crear lotes nuevos
  repetidos por cada donación rompería ese contrato de unicidad.
- **Alternatives considered**:
  - Crear una fila duplicada por donación: incompatible con el índice único actual.
  - Reemplazar el servicio o el modelo de Stock: rechazado por la solicitud; se añade
    solo la operación de ingreso faltante.

### D4. Agregar tipo de alimento a Producto y conservar instantáneas de la recepción

- **Decisión**: Añadir el tipo de alimento al modelo de Producto existente y a su
  auditoría. Cada ítem conserva en su historial el nombre, tipo y unidad de medida
  recibidos al confirmar. Los productos manuales se registran como productos
  identificables para que Stock y Remito puedan reutilizar sus IDs. Productos históricos
  sin clasificación permanecen sin tipo hasta que depósito los clasifique; altas y
  recepciones nuevas exigen un valor.
- **Rationale**: Producto ya posee nombre, descripción, unidad y estado; Stock solo
  hace referencia a producto. El tipo de alimento no existe hoy, por lo que su
  incorporación es una extensión del modelo, no una feature paralela.
- **Alternatives considered**:
  - Guardar tipo y unidad solo en la solicitud: rechazado porque el detalle histórico y
    el comprobante perderían la información.
  - Cambiar Stock para duplicar los datos del catálogo: rechazado porque alteraría
    innecesariamente su modelo existente.

### D5. Exponer DTOs, filtros paginados y acceso restringido a depósito

- **Decisión**: Nuevos recursos bajo `/api/donantes` y `/api/donaciones`; ampliar
  `/api/productos` para altas, actualización y baja lógica; conservar `/api/stock` como
  listado y modificarlo solo a través de la confirmación de donaciones. Extender
  `/api/facturas` para leer comprobantes. Declarar explícitamente el rol
  `user_deposito` para cada método nuevo. Listados usan `pagina` y `tamano`, con filtros
  de fecha, donante, nombre, producto y vencimiento pertinentes.
- **Rationale**: Los listados existentes ya usan esos nombres, `Paginacion`,
  `PaginaRespuesta` y autorización central en `ConfiguracionSeguridad`.
- **Alternatives considered**:
  - Introducir un segundo formato de paginación o DTOs de entidad: rechazado por
    inconsistencia con la API existente.
  - Permitir acceso de lectura a otros roles: rechazado porque el requisito limita las
    consultas de esta feature al depósito.

## Supuestos que deben validarse durante la implementación

1. Los comprobantes nuevos usan `estado=EMITIDA` e `importe=null`; no representan deuda,
   precio ni una suma económica de alimentos. El importe y demás valores de las
   facturas históricas se conservan.
2. No se especifica un formato de número de comprobante. Se usa el ID autoincremental
   de Factura como número visible, sin alterar los números históricos.
3. Si para una recepción se cambia nombre, tipo o unidad de un producto de catálogo,
   debe crearse/resolverse un Producto separado para que cada existencia tenga una
   única unidad base; no se mutará la ficha original ni se hará conversión automática.
4. El catálogo seed incluye leche (`Lácteo`, litros), harina (`Harina`, kilos) y
   galletitas (`Galletitas`, paquetes). Son valores de texto descriptivos: no definen
   una taxonomía institucional ni implican conversiones.
5. Productos preexistentes pueden permanecer sin `tipoAlimento` hasta que depósito los
   clasifique; el tipo sí es obligatorio para productos nuevos y recepciones.
6. Para donantes, se mantiene como obligatorio el nombre y opcionales los datos de
   contacto no definidos en la solicitud. Baja de donante/producto es lógica para
   preservar auditoría e historial.
7. La cantidad se expresa en la unidad base del producto y se conserva la precisión
   actual de Stock (tres decimales); no se inventan conversiones entre unidades.
8. Las líneas repetidas del mismo producto y vencimiento se conservan individualmente
   en la donación y su comprobante, y suman al único lote de Stock.
9. El registro de una donación es inmediato y confirmado; no se agrega ciclo de borrador
   porque no está requerido y la atomicidad es de la operación completa.

## Fuentes del repositorio revisadas

- `src/main/java/com/group6/BancoAlimentos/factura/modelo/Factura.java`
- `src/main/java/com/group6/BancoAlimentos/factura/repository/FacturaRepository.java`
- `src/main/java/com/group6/BancoAlimentos/factura/controller/FacturaController.java`
- `src/main/resources/db/migration/V6__crear_proyeccion_consulta_facturas.sql`
- `src/main/java/com/group6/BancoAlimentos/producto/modelo/Producto.java`
- `src/main/java/com/group6/BancoAlimentos/stock/modelo/Stock.java`
- `src/main/java/com/group6/BancoAlimentos/stock/repository/StockRepository.java`
- `src/main/java/com/group6/BancoAlimentos/remito/service/ConfirmacionRemitoService.java`
- `src/main/java/com/group6/BancoAlimentos/Security/ConfiguracionSeguridad.java`
- `.specify/memory/constitution.md`
