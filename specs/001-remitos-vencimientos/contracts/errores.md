# Contrato de errores

Todas las respuestas de error usan el mismo objeto JSON:

```json
{
  "fechaHora": "2026-10-26T12:30:00Z",
  "estado": 422,
  "codigo": "REGLA_NEGOCIO",
  "mensaje": "La existencia seleccionada está vencida.",
  "ruta": "/api/remitos/12/detalles",
  "erroresCampo": []
}
```

`erroresCampo` contiene objetos `{ "campo": "...", "mensaje": "..." }` para errores de
validación; en otros errores es una lista vacía.

| Estado HTTP | Uso |
|---|---|
| `400` | DTO inválido o parámetros mal formados |
| `401` | Credenciales ausentes o token inválido/expirado |
| `403` | Rol no autorizado |
| `404` | Recurso inexistente o no visible para el usuario |
| `422` | `ReglaNegocioException`: vencimiento, stock, duplicados o remito vacío |
| `500` | Error inesperado; sin detalles internos ni secretos |

Las transiciones de negocio inválidas, como confirmar un remito ya confirmado, también
se representan con HTTP `422` para mantener el contrato de `ReglaNegocioException`.
Una colisión concurrente que se traduzca en una regla de dominio debe conservar ese
mismo estado.

Los mensajes para el usuario no deben exponer consultas SQL, stack traces, credenciales
ni datos de otros usuarios o instituciones.
