# Modelo de datos: Ajustes manuales de stock

## Ajuste

Registro de negocio que conserva el antes/después de cada operación manual en un lote existente. Se almacena en `ajustes_stock` y se audita con Hibernate Envers en `ajustes_stock_aud`; las modificaciones/bajas solo se permiten sin revisiones de stock posteriores.

| Campo | Tipo lógico / SQL | Reglas |
|---|---|---|
| `id` | Long / BIGINT | PK autogenerada; identidad estable del evento. |
| `stockId` | Long / BIGINT | Obligatorio; FK a `stock.id`. Identifica el lote concreto que fue bloqueado y ajustado. |
| `productoId` | Long / BIGINT | Obligatorio; FK a `productos.id`; snapshot del lote al momento del ajuste. |
| `fechaVencimiento` | LocalDate / DATE nullable | Snapshot del lote; nulo si el stock existente no tiene vencimiento. |
| `usuarioId` | Long / BIGINT | Obligatorio; FK a `usuarios.id`; siempre provisto desde el principal autenticado. |
| `fechaHora` | Instant / TIMESTAMP(6) | Obligatoria e inmutable; generada por el servidor en UTC. |
| `tipo` | enum / VARCHAR(20) | `CORRECCION` o `BAJA`; editable con auditoría. |
| `cantidadAnterior` | BigDecimal / DECIMAL(12,3) | Obligatoria, no negativa; valor previo conservado como base para edición. |
| `cantidadNueva` | BigDecimal / DECIMAL(12,3) | Obligatoria, no negativa; saldo final aplicado a `Stock`. |
| `motivo` | String / longitud a definir | Obligatorio y no vacío después de recortar espacios. El usuario lo proporciona. La longitud máxima queda pendiente de confirmar antes de añadir una restricción de negocio. |

### Relaciones e instantáneas

- Cada ajuste pertenece a un lote (`stockId`) y conserva `productoId` y `fechaVencimiento` como valores históricos consultables, además de las claves foráneas que aseguran referencias válidas al momento de insertar.
- El usuario es una referencia al registro de usuario; el snapshot de identidad de usuario será su `usuarioId` (no se congela el nombre visible, que puede cambiar).
- La entidad no se elimina en cascada desde stock, producto o usuario. Las FKs deben impedir eliminar registros referenciados; los flujos existentes deben mantener esos datos.
- No se cambia `EstadoStock` como efecto implícito del ajuste. La elegibilidad para remitos conserva las validaciones actuales.

## Tipo de ajuste

`TipoAjuste`:

- `CORRECCION`: `cantidad` solicitada es la cantidad objetivo real; acepta cero, rechaza valores negativos y conserva precisión de hasta tres decimales. `cantidadNueva = cantidadSolicitada`.
- `BAJA`: `cantidad` solicitada es la cantidad a descontar; debe ser mayor que cero y menor o igual al saldo anterior. `cantidadNueva = cantidadAnterior - cantidadSolicitada`.

Las dos ramas deben comprobar el resultado no negativo. El motivo se valida en el borde y en la regla de servicio; su valor persistido se recorta. Una operación válida puede producir el mismo saldo que tenía (por ejemplo, corrección al saldo actual) y aun así registra el evento solicitado.

## Invariantes y transacción

1. Solo el rol `user_deposito` puede crear, consultar, modificar o eliminar estos registros.
2. Solo se puede ajustar un `Stock` existente; `stockId`, producto y usuario deben referenciar filas existentes.
3. La consulta del lote se ejecuta con bloqueo pesimista de escritura antes de leer `cantidadAnterior`.
4. Validación, actualización de `Stock` e inserción de `Ajuste` se realizan en la misma transacción. Cualquier error deja ambos datos sin cambios.
5. El nuevo saldo no puede ser negativo; además de la validación del service, se mantiene la restricción de stock existente.
6. La edición y baja bloquean el lote y consultan revisiones Envers: cualquier modificación posterior del stock impide alterar el ajuste. Una edición aplica el nuevo resultado desde `cantidadAnterior`; una baja restaura dicho valor. Envers conserva las revisiones anteriores.
7. El vencimiento nulo es válido porque el modelo existente de stock lo permite. El ajuste conserva ese valor. Las consultas por fecha filtran un vencimiento concreto; con parámetro omitido no se filtra por vencimiento.
8. Un remito en borrador no reserva stock. Tras un ajuste, la confirmación existente vuelve a validar el saldo y solo descuenta si el remito puede confirmarse íntegramente.

## Consulta paginada

La búsqueda de ajustes acepta filtros opcionales combinables:

- `productoId`: coincidencia exacta del snapshot de producto.
- `fechaVencimiento`: coincidencia exacta; omitirlo no filtra por vencimiento.
- `fechaDesde` y `fechaHasta`: fechas inclusivas en UTC derivadas de `fechaHora`.
- `usuarioId`: coincidencia exacta.
- `tipo`: `CORRECCION` o `BAJA`.
- `pagina` y `tamano`: convención de `Paginacion`; tamaño máximo 100.

El orden predeterminado será fecha/hora descendente y, como desempate estable, ID descendente. Si se omiten ambos límites de fecha no se aplica restricción temporal. Rangos invertidos deben rechazarse como solicitud inválida.

## Migración

`V9__crear_ajustes_stock.sql`, después de V8, crea la tabla del evento, su tabla de Envers, restricciones de saldo, FKs e índices para filtrar por producto, fecha de vencimiento, fecha/hora, usuario y tipo. `V10__corregir_tipo_ajuste_stock.sql` elimina la restricción de tipo mal escrita de V9 sin modificar esa migración histórica; la API limita el valor a `CORRECCION` o `BAJA` mediante el enum del DTO y la validación del service. La tabla de auditoría sigue el patrón de las tablas `*_aud`: clave `(id, REV)`, `REVTYPE` y FK a `REVINFO`.
