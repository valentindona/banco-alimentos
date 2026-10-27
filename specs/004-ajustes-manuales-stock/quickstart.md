# Quickstart: validar ajustes manuales de stock

## Requisitos

- JDK 21.
- La feature implementada en la rama `004-ajustes-manuales-stock`.
- Usar el perfil de pruebas existente: H2 en modo MySQL, Flyway habilitado y `ddl-auto: validate`.

No se requiere MySQL local ni modificar las credenciales de desarrollo. Las pruebas deben ejecutarse con el wrapper del repositorio desde la raíz.

## Ejecutar validaciones

En PowerShell:

```powershell
.\mvnw.cmd -Dtest=AjusteServiceTest,AjusteServiceConcurrenciaTest,AjusteControllerIntegracionTest,AjusteMigracionTest,AjusteRepositoryTest,AjusteConsultaRepositoryTest,AjustePermisosIntegracionTest,ContratoAjustesOpenApiTest test
```

Las pruebas de servicio cubren las reglas y rollback; la prueba de concurrencia comprueba el bloqueo por lote; las de integración validan endpoints, permisos, filtros, migración, consultas y Envers sobre H2.

## Escenarios de aceptación

1. Preparar un usuario `user_deposito`, producto y lote con saldo `10.000`.
2. `POST /api/ajustes` con `tipo=BAJA`, `cantidad=2.500` y motivo válido debe responder `201`, guardar saldo nuevo `7.500` y devolver cantidad anterior/nueva, producto, lote, autor, fecha y motivo.
3. Registrar `tipo=CORRECCION` con `cantidad=0` debe dejar el lote en cero y registrar el evento. Una corrección negativa, una baja igual a cero, una baja superior al saldo o un motivo vacío debe fallar sin cambiar lote ni historial.
4. Dos operaciones concurrentes sobre el mismo `stockId` deben serializarse. El saldo no puede ser negativo y cada evento debe registrar su saldo anterior y nuevo efectivo.
5. `GET /api/ajustes` debe combinar producto, vencimiento, fechaDesde/fechaHasta, usuario y tipo; la respuesta respeta paginación y un filtro sin coincidencias da una página vacía.
6. Un usuario institucional o de tesorería no puede registrar ni consultar: debe recibir `403` sin cambios en stock ni historial. Una petición sin autenticación recibe `401`.
7. El ajuste a un lote de un remito `BORRADOR` debe permitirse. Si el saldo queda por debajo de lo detallado, confirmar el remito debe fallar con regla de negocio; el remito sigue en borrador y el stock conserva el saldo resultante del ajuste. Si hay suficiente saldo y el lote es elegible, se conserva el flujo de confirmación existente.
8. `PUT /api/ajustes/{ajusteId}` debe recalcular el saldo usando `cantidadAnterior` solo si el saldo actual sigue coincidiendo con `cantidadNueva` y Envers no registra revisiones posteriores del lote. Debe conservar la revisión previa y crear una nueva; cualquier conflicto deja el stock intacto.
9. `DELETE /api/ajustes/{ajusteId}` debe restaurar `cantidadAnterior` bajo las mismas condiciones y conservar la revisión Envers de baja. Movimientos posteriores deben impedir la operación.
10. V9 crea las tablas principal/audit, relaciones, restricciones de saldo e índices; V10 retira el constraint de tipo mal escrito sin modificar V9. La API sigue aceptando únicamente los tipos del enum (`CORRECCION` y `BAJA`). Flyway aplica ambas migraciones en H2 y MySQL.

Para contratos, campos de solicitud, filtros, estados HTTP y respuesta paginada, consultar [contracts/ajustes-stock.openapi.yaml](./contracts/ajustes-stock.openapi.yaml). Para invariantes y campos persistidos, consultar [data-model.md](./data-model.md).

## Resultado esperado

El comando Maven termina con código cero. Los escenarios negativos comprueban que falla la operación y que tanto el stock como el número de registros de ajuste permanecen intactos; las operaciones exitosas muestran el resultado registrado y el snapshot de auditoría esperado.
