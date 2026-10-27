# Modelo de datos: Registro de donaciones

## Donante

Identifica a una persona u organización que entrega alimentos.

| Campo | Tipo conceptual | Reglas |
|-------|-----------------|--------|
| id | Identificador | Generado por el sistema. |
| nombre | Texto | Obligatorio, no vacío. |
| correo | Texto | Opcional; validar formato si se informa. |
| telefono | Texto | Opcional. |
| activo | Booleano | Por defecto activo; baja lógica. |

**Relaciones**: un donante puede realizar muchas donaciones. Una baja no elimina ni
modifica las recepciones ni los comprobantes anteriores.

**Auditoría**: Envers registra altas y modificaciones, incluido el cambio de estado.

## Donacion

Recepción confirmada de mercadería en una fecha. En esta versión no tiene borrador ni
transición posterior: se registra únicamente al completar la transacción con stock e
comprobante.

| Campo | Tipo conceptual | Reglas |
|-------|-----------------|--------|
| id | Identificador | Generado por el sistema. |
| donanteId | Referencia a Donante | Obligatoria; el donante debe estar activo al confirmar. |
| fecha | Fecha | Obligatoria; corresponde a la fecha de recepción. |
| items | Lista de ItemDonacion | Al menos una línea. |

**Relaciones**: muchas donaciones pertenecen a un donante; una donación contiene una o
más líneas y tiene exactamente un comprobante. Una donación confirmada no se edita ni
se anula en el alcance inicial.

**Auditoría**: Envers registra la donación y sus cambios dentro de la transacción de
confirmación.

## ItemDonacion

Detalle inmutable de un producto recibido; conserva los datos confirmados aunque la
ficha global del producto cambie después.

| Campo | Tipo conceptual | Reglas |
|-------|-----------------|--------|
| id | Identificador | Generado por el sistema. |
| donacionId | Referencia a Donacion | Obligatoria. |
| productoId | Referencia a Producto | Obligatoria; producto activo al confirmar. |
| productoNombre | Texto snapshot | Obligatorio. |
| tipoAlimento | Texto snapshot | Obligatorio. |
| unidadMedida | Texto snapshot | Obligatoria. |
| cantidad | Decimal | Obligatoria, mayor que cero, hasta tres decimales según Stock. |
| fechaVencimiento | Fecha | Obligatoria y posterior a la fecha de confirmación. |

**Relaciones**: cada línea pertenece a una donación, referencia un producto y
corresponde a una combinación de producto y vencimiento de Stock. Líneas del mismo
producto con vencimientos distintos son diferentes lotes.

**Auditoría**: Envers registra la línea. No se modifican líneas después de confirmar.

## Producto (extensión)

Se reutiliza `producto.Producto`; se agrega `tipoAlimento` y se conserva la unidad
existente.

| Campo existente o nuevo | Tipo conceptual | Reglas |
|-------------------------|-----------------|--------|
| id, nombre, descripcion, unidadMedida, activo | Existentes | Conservar significado, validación e historial actuales. |
| tipoAlimento | Nuevo texto | Obligatorio para altas nuevas y líneas recibidas; valores iniciales acordados en el seed. |

**Relaciones**: un producto puede aparecer en muchas líneas de donación y existencias.
No se elimina físicamente si tiene existencias o historial. El tipo y unidad guardados
en ItemDonacion forman el snapshot, no sustituyen la ficha global.

**Auditoría**: ampliar `productos_aud` para la nueva propiedad sin borrar revisiones.
Para productos históricos cuyo tipo aún no exista, la migración no debe inventar una
clasificación; la política de backfill/selección queda anotada como supuesto en el
plan.

## Stock (extensión de uso)

Se reutiliza `stock.Stock`; no se agrega una segunda tabla de existencias.

| Campo existente | Regla para ingreso |
|-----------------|--------------------|
| productoId | Debe corresponder al producto resuelto para el ítem. |
| fechaVencimiento | Debe coincidir con el vencimiento futuro de la línea. |
| cantidadDisponible | Sumar exactamente la cantidad recibida; conservar escala 3 y restricción no negativa. |
| estado | La nueva existencia queda elegible según los estados/reglas existentes. |

La combinación producto-vencimiento mantiene el índice único existente. El incremento
se solicita mediante un servicio público de Stock que participa en la transacción de
DonacionService. La auditoría existente de Stock registra el cambio.

## Factura (comprobante, extensión compatible)

Se reutiliza `factura.Factura` y la tabla `facturas`. Las filas actuales representan
proyecciones con institución; V6 no las relaciona con Remito. Los valores históricos
de `institucionId`, `numero`, `fecha`, `estado` e `importe` se conservan.

| Campo | Tipo conceptual | Reglas |
|-------|-----------------|--------|
| id | Identificador | Existente. |
| numero, fecha, estado, importe | Campos existentes | Preservar valores legados. Los comprobantes nuevos usan el ID autoincremental como `numero`, estado `EMITIDA` e importe nulo (sin valor económico). |
| institucionId | Referencia opcional | Conservar para filas existentes; nula en comprobantes de donaciones. |
| donanteId | Referencia opcional a Donante | Obligatoria para comprobantes nuevos; nula en filas históricas. |
| donacionId | Referencia opcional a Donacion | Obligatoria para comprobantes nuevos; relación única; nula en filas históricas. |
| detalle | Proyección de ItemDonacion | Incluye producto, tipo, unidad, cantidad y vencimiento; lectura inmutable desde la donación asociada. |

**Relaciones**: la donación y el donante se asocian explícitamente a la factura de
recepción. El detalle se obtiene de las líneas inmutables de esa donación; no se
duplican líneas de inventario en una nueva tabla de factura.

**Migración**: V7 permite `facturas.institucion_id` nulo y agrega las referencias
opcionales, índice único por donación, claves foráneas y columnas equivalentes en
`facturas_aud`. Ninguna fila histórica se transforma ni pierde su institución.

## Límites transaccionales y validaciones comunes

- La entrada, sus líneas, incrementos de Stock, auditorías y Factura se confirman o se
  revierten juntos.
- Debe existir al menos una línea; cantidades positivas; nombre, tipo y unidad completos;
  producto y donante válidos/activos; vencimiento futuro.
- Una falla al crear cualquiera de esos registros provoca rollback; no se devuelve una
  respuesta de éxito parcial.
- Las reglas de vencimiento de Remito no cambian.
- Baja lógica de donante/producto para conservar FK, historial y auditoría.
- Listados paginados y filtrados con los parámetros de [contracts/openapi.yaml](./contracts/openapi.yaml).
