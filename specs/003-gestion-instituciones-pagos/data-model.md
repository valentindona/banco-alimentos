# Data Model: Gestión de instituciones y estados de pago

## Institucion

Extensión de la entidad de negocio existente `instituciones`; no se crea una entidad paralela.

| Campo | Tipo conceptual | Reglas |
|---|---|---|
| `id` | Identificador | Existente; único e inmutable. |
| `nombre` | Texto | Existente; obligatorio, máximo 150 caracteres; no se permite duplicar sin distinguir mayúsculas/minúsculas al crear o cambiar el nombre. |
| `domicilio` | Texto | Existente; obligatorio, máximo 250 caracteres. |
| `tipo` | Texto | Nuevo; obligatorio, máximo 80 caracteres; texto libre confirmado por el usuario, normalizado quitando espacios exteriores. Los registros existentes se migran con `Sin especificar` (supuesto de migración). |
| `estadoPago` | Estado controlado | Existente; obligatorio; uno de `AL_DIA`, `EN_DEUDA`, `PARTICULAR`. En una institución nueva se inicializa `AL_DIA` (decisión confirmada). Solo tesorería puede cambiarlo. |
| `activa` | Booleano | Existente; valor inicial verdadero. La baja cambia el valor a falso; no se borra el registro. |

### Relaciones y conservación

- `remitos.institucion_id` referencia a `instituciones.id`; no se elimina la institución al darla de baja.
- `usuarios.institucion_id` también puede referenciar una institución; la baja no elimina usuarios ni rompe el vínculo.
- Envers registra cambios de la institución en `instituciones_aud`. La migración incorpora `tipo` a la tabla principal y a la tabla de auditoría.
- Las tablas y reglas de remitos no cambian.

### Validaciones y ciclo de vida

- El DTO de alta/modificación valida nombre, domicilio y tipo no vacíos y sus longitudes; las entradas usan `@Valid`.
- Crear rechaza un nombre ya usado, sin diferenciar mayúsculas/minúsculas.
- Actualizar una institución inexistente o dada de baja se rechaza como regla de negocio.
- Dar de baja una institución activa la desactiva; repetir la baja no debe causar un error ni borrar datos.
- El estado de pago no se incluye en DTOs de alta/modificación. El cambio usa una operación propia de tesorería; el enum existente impide valores fuera del conjunto permitido.
- Listados de operación incluyen instituciones activas y aplican en conjunto los filtros de nombre, tipo y estado. Se ordenan por id ascendente y usan la paginación común (20 por defecto, máximo 100).

## Estados de pago

| Valor | Significado |
|---|---|
| `AL_DIA` | La cuota institucional está al día. |
| `EN_DEUDA` | La cuota institucional está en deuda; no bloquea la recepción de entregas ni la creación de remitos. |
| `PARTICULAR` | Condición particular de la institución, no equivalente a deuda. |

El estado representa solo el valor vigente; no se guardan importes, cobros ni historial de pagos.

## DTOs previstos

- **Solicitud de institución**: `nombre`, `domicilio`, `tipo`; sin `estadoPago`, `activa` ni `id`.
- **Respuesta de institución**: `id`, `nombre`, `domicilio`, `tipo`, `estadoPago`, `activa`.
- **Solicitud de estado de pago**: `estadoPago`; obligatorio y limitado al enum existente.
- **Respuesta de listado**: formato común `PaginaRespuesta`, con contenido y metadatos de paginación.
