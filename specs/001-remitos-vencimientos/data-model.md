# Data Model: Entrega de mercadería con remitos y control de vencimientos

## Convenciones

- Identificadores son `Long`; fechas de negocio son fechas locales sin hora.
- Cantidades e importes se representan con decimales de precisión fija. La cantidad
  debe ser mayor que cero al solicitar una línea y nunca negativa en stock.
- Los DTOs de API no exponen entidades JPA. Las reglas y transiciones se ejecutan en
  el service de la feature correspondiente.
- Las referencias entre features se mantienen como identificadores y se resuelven por
  interfaces de service; una feature no importa entidades ni repositories internos de
  otra.
- Se aplica Envers a `Producto`, `Stock`, `Institucion`, `Remito`, `DetalleRemito` y
  `Factura`. `Usuario` y `Notificacion` quedan fuera de la auditoría de negocio.

## Entidades

### Producto

Representa el artículo del catálogo disponible para entregar.

| Campo | Regla |
|---|---|
| `id` | Identificador único |
| `nombre` | Obligatorio; se usa para búsquedas y filtros |
| `descripcion` | Opcional |
| `unidadMedida` | Obligatoria; unidad en que se expresa la existencia |
| `activo` | Indica si se ofrece para nuevas líneas de remito |

Un producto puede tener varias existencias, cada una con vencimiento y cantidad
independientes. El vencimiento de una existencia no inactiva el producto completo.

### Stock

Representa la existencia agregada de un producto para una fecha de vencimiento.

| Campo | Regla |
|---|---|
| `id` | Identificador único de existencia |
| `productoId` | Obligatorio; referencia a Producto |
| `fechaVencimiento` | Puede ser nula para conservar datos incompletos; en tal caso no es entregable |
| `cantidadDisponible` | Decimal mayor o igual a cero |
| `estado` | `DISPONIBLE`, `VENCIDO` o `SIN_VENCIMIENTO` |
| `fechaActualizacion` | Última actualización de cantidad o estado |

La combinación producto/fecha de vencimiento identifica la existencia fechada y tiene
restricción única. Para una fecha nula, el service reutiliza la existencia sin
vencimiento del producto; no se entrega hasta completar el dato. La confirmación
bloquea las filas requeridas en orden ascendente de identificador. La actualización
programada marca `VENCIDO` desde la fecha inclusive;
la validación de entrega también compara la fecha en tiempo de confirmación y no depende
de que la tarea ya se haya ejecutado. Una existencia sin fecha es no elegible.

### Institucion

Representa a la organización beneficiaria y su estado de pago.

| Campo | Regla |
|---|---|
| `id` | Identificador único |
| `nombre` | Obligatorio |
| `domicilio` | Obligatorio para identificar el destino en el remito |
| `estadoPago` | `AL_DIA`, `EN_DEUDA` o `PARTICULAR` |
| `activa` | Indica si se puede seleccionar para nuevos remitos |

El estado `EN_DEUDA` no bloquea la entrega. Se asume que nombre y domicilio son los
datos mínimos obligatorios para esta feature; campos de contacto o identificación legal
quedan para una futura especificación de Institucion.

### Remito

Registra una entrega a una institución. El destino se conserva también como una
instantánea para que un cambio posterior de domicilio o nombre institucional no
reescriba los datos del remito ya preparado.

| Campo | Regla |
|---|---|
| `id` | Identificador único |
| `institucionId` | Obligatorio; referencia a Institucion |
| `institucionNombre` | Instantánea del nombre validado al preparar |
| `institucionDomicilio` | Instantánea del domicilio validado al preparar |
| `fechaEntrega` | Obligatoria |
| `estado` | `BORRADOR` o `CONFIRMADO` |
| `fechaCreacion` | Obligatoria |
| `fechaConfirmacion` | Nula mientras sea borrador |
| `creadoPorUsuarioId` | Usuario de depósito que inició el remito |

Solo un borrador puede recibir detalles o confirmarse. La transición a confirmado y los
descuentos de stock se realizan en una misma transacción. No se reserva stock durante el
borrador. Una institución en deuda es válida como destino.

### DetalleRemito

Representa una línea de mercadería en un remito.

| Campo | Regla |
|---|---|
| `id` | Identificador único |
| `remitoId` | Obligatorio; referencia a Remito |
| `stockId` | Obligatorio; existencia producto/vencimiento elegida explícitamente |
| `productoId` | Referencia informativa al producto de la existencia |
| `fechaVencimiento` | Instantánea del vencimiento seleccionado |
| `productoNombre` | Instantánea del nombre del producto |
| `cantidad` | Obligatoria y mayor que cero |

La combinación remito/stock es única. Puede incluirse el mismo producto en dos líneas
distintas solo si corresponden a existencias con vencimientos diferentes.

### Usuario

Cuenta mínima para autenticación y autorización.

| Campo | Regla |
|---|---|
| `id` | Identificador único |
| `nombreUsuario` | Obligatorio y único |
| `claveHash` | Hash adaptativo; nunca se devuelve en DTOs |
| `rol` | `user_deposito`, `user_institucional` o `user_tesoreria` |
| `institucionId` | Requerido para el rol institucional; nulo para los demás |
| `activo` | Cuenta habilitada o deshabilitada |

Las cuentas se crean mediante provisionamiento protegido, sin registro público. La
clave JWT y credenciales iniciales se proveen por configuración externa.

### Notificacion

Aviso persistido, visible únicamente para el rol de depósito.

| Campo | Regla |
|---|---|
| `id` | Identificador único |
| `stockId` | Existencia que originó el aviso |
| `tipo` | `POR_VENCER` o `VENCIDO` |
| `audiencia` | `user_deposito` |
| `fechaCreacion` | Fecha/hora de generación |
| `mensaje` | Texto breve con producto, vencimiento y cantidad |

Hay como máximo un aviso por existencia y tipo de evento. No se requiere estado de
lectura ni notificación fuera de la aplicación.

### Factura (consulta mínima)

Proyección estrictamente de lectura requerida para consulta de depósito; no contiene
operaciones de generación, pago ni modificación.

| Campo | Regla |
|---|---|
| `id` | Identificador único |
| `numero` | Referencia de factura |
| `institucionId` | Institución facturada |
| `fecha` | Fecha de factura |
| `estado` | Estado de factura para consulta |
| `importe` | Importe total |

Los nombres y forma exacta de estos datos son supuestos provisionales y deberán
alinearse con una futura spec de facturación.

## Relaciones y restricciones

- `Producto 1 ── N Stock`; cada existencia tiene como máximo un vencimiento fechado por
  producto.
- `Institucion 1 ── N Remito`; el remito conserva instantáneas del destino.
- `Remito 1 ── N DetalleRemito`; un borrador vacío puede existir, pero no confirmarse.
- `Stock 1 ── N DetalleRemito`; solo se permite un detalle por existencia y remito.
- `Usuario 1 ── N Remito`; el creador de cada remito debe tener rol de depósito.
- `Institucion 1 ── N Usuario` institucional; cada usuario institucional se asocia con
  una única institución.
- `Stock 1 ── N Notificacion`, limitado a un evento de cada tipo por existencia.
- Ningún descuento deja `cantidadDisponible` negativa. Toda línea se valida de nuevo
  para existencia, estado, fecha y cantidad al confirmar.
- Toda consulta institucional de remitos se filtra por el `institucionId` asociado al
  token autenticado; nunca por un identificador institucional arbitrario enviado por el
  cliente.

## Proceso diario

Una ejecución diaria busca todas las existencias con cantidad positiva y vencimiento
hoy o anterior que aún no se marcaron como vencidas, las bloquea y crea un aviso
`VENCIDO` si no existía. También crea avisos `POR_VENCER` para existencias con fecha
entre mañana y tres días inclusive. La operación es repetible e idempotente para
recuperarse de ejecuciones repetidas o interrupciones.
