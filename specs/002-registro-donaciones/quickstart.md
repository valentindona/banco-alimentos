# Validación rápida: registro de donaciones

Guía de aceptación de la entrada de mercadería y el comprobante. Los detalles de
campos y respuestas están en [contracts/openapi.yaml](./contracts/openapi.yaml), y
las reglas del modelo en [data-model.md](./data-model.md).

## Prerrequisitos

- Java 21.
- Las dependencias Maven descargadas por el wrapper.
- No se necesita iniciar Docker para las pruebas con H2.
- Para ejecutar la aplicación fuera de las pruebas, configurar `DB_URL`,
  `DB_USERNAME`, `DB_PASSWORD` y `JWT_SECRET` en el entorno local; no guardar sus
  valores en el repositorio.

## Validación automatizada

Ejecutar pruebas unitarias, de integración y migraciones:

```powershell
.\mvnw.cmd clean verify
```

Resultado esperado: build exitoso, migraciones V1–V7 aplicadas a un esquema H2 limpio
y pruebas de reglas, persistencia, permisos, atomicidad, auditoría y contratos sin
fallas. Las pruebas específicas para MySQL requieren una base de pruebas disponible y
no forman parte de la verificación H2.

## Escenarios de aceptación

1. **Donación válida**: autenticar como `user_deposito`; crear un donante activo; enviar
   una recepción con al menos dos productos y vencimientos futuros. Verificar respuesta
   con donación, líneas y comprobante asociado; consultar `/api/stock` y comprobar que
   cada par producto-vencimiento aumentó exactamente por la cantidad recibida.
2. **Catálogo y producto manual**: consultar `/api/productos`; seleccionar un producto
   precargado y revisar tipo/unidad; enviar un producto manual completo. Confirmar que
   ambos se pueden consultar y que cualquier dato de línea confirmado se preserva en el
   detalle sin cambiar los datos del producto original.
3. **Atomicidad**: enviar líneas con cantidad no positiva, vencimiento ausente/pasado,
   producto/donante inexistente o donante inactivo. Esperar error de validación o regla
   de negocio; volver a consultar donaciones, stock y facturas y confirmar que no hay
   escrituras parciales.
4. **Lotes repetidos**: registrar el mismo producto con dos vencimientos futuros.
   Verificar dos líneas históricas y dos existencias correspondientes; una repetición
   del mismo lote suma la cantidad a la existencia única existente.
5. **Comprobante**: consultar `/api/facturas` por donante y fecha; verificar el vínculo
   con la donación y el detalle completo. Intentar operaciones de escritura sobre
   facturas; no deben estar expuestas.
6. **Filtros**: consultar donantes por nombre; donaciones por donante/fecha/producto/
   vencimiento; productos por nombre/tipo/unidad; stock por producto/fecha; facturas por
   donante/fecha/producto/vencimiento. Verificar que todas las respuestas sean paginadas.
7. **Permisos**: repetir lectura y escritura con `user_tesoreria` y
   `user_institucional`. Las rutas de esta feature deben responder `403`, sin persistir
   cambios.
8. **Regresión de remitos**: ejecutar las pruebas existentes de confirmación y
   vencimientos; el stock recibido sigue siendo elegible solo bajo las reglas actuales,
   y productos vencidos continúan bloqueados.

## Migraciones y contrato

- Aplicar V1–V7 en una base H2 limpia. Verificar que las filas de Factura existentes
  conservan institución, número, fecha, estado e importe.
- Con MySQL de pruebas disponible, ejecutar Flyway sobre una base limpia y sobre una
  copia con facturas legadas antes de liberar V7; comparar índices, restricciones y
  tablas de auditoría con H2.
- Validar que el contrato OpenAPI de esta feature se integra sin divergencias en
  `src/main/resources/openapi.yaml`; `x-roles` identifica `user_deposito` para cada
  operación nueva.

Los valores de ejemplo del seed son datos ficticios de prueba. Los estados neutrales de
comprobante, numeración, unidad y tipo del catálogo están sujetos a los supuestos
documentados en el plan.
