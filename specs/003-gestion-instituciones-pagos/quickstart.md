# Quickstart: Gestión de instituciones y estados de pago

## Prerrequisitos

- Java 21 y el wrapper Maven del repositorio.
- Configuración de prueba existente en `src/test/resources/application-test.yml`; las pruebas de integración usan H2, Flyway y el perfil `test`.

## Validación automatizada

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd "-Dtest=ConsultaInstitucionServiceTest,InstitucionRepositoryTest,InstitucionServiceTest,InstitucionControllerIntegracionTest,InstitucionEstadoPagoIntegracionTest,InstitucionConsultaIntegracionTest,AutorizacionEndpointTest,ContratoInstitucionesOpenApiTest" test
```

Las pruebas focalizadas verifican:

- Migraciones aplicadas a H2 y esquema validado por JPA.
- Reglas de alta, actualización, nombres duplicados, baja lógica y cambio de estado de pago.
- Filtros combinados por nombre, tipo y estado, exclusión de instituciones inactivas y paginación con tamaño por defecto 20 y máximo 100.
- Consulta de listado y detalle para `user_deposito`, `user_tesoreria` y `user_institucional`; el rol de depósito no puede modificar instituciones ni el estado de pago.
- Contrato OpenAPI de ABM, actualización de estado de pago, filtros, respuestas y roles.
- Creación de remitos para instituciones en deuda y conservación de remitos históricos tras la baja lógica.

Para ejecutar toda la suite:

```powershell
.\mvnw.cmd test
```

## Escenarios manuales de API

Autenticarse con una cuenta de cada rol y utilizar el token Bearer correspondiente.

1. Como `user_institucional`, crear una institución con nombre, domicilio y tipo; esperar HTTP 201 y una respuesta con `estadoPago: AL_DIA` y `activa: true`.
2. Como `user_deposito`, `user_tesoreria` o `user_institucional`, consultar listado y detalle. Combinar `nombre`, `tipo` y `estadoPago`; comprobar que se aplican todos los filtros, que un filtro sin coincidencias devuelve contenido vacío y que la respuesta incluye metadatos de paginación.
3. Como `user_tesoreria`, enviar `PATCH /api/instituciones/{institucionId}/estado-pago` con `{"estadoPago":"EN_DEUDA"}`; verificar el valor en la respuesta y en una consulta posterior. Repetir el mismo cambio y esperar éxito.
4. Como `user_deposito`, intentar crear, actualizar o dar de baja una institución, y cambiar su estado de pago; esperar HTTP 403 en cada operación y verificar que no hubo cambios.
5. Crear un remito para una institución en estado `EN_DEUDA`; comprobar que se crea el borrador y que las validaciones normales de remitos siguen aplicándose.
6. Como `user_institucional`, dar de baja una institución con remitos; comprobar que deja de aparecer en el padrón activo y que sus remitos históricos siguen consultables.

Los paths, cuerpos, parámetros, roles y respuestas están detallados en [contracts/openapi.yaml](contracts/openapi.yaml); los campos y sus reglas están en [data-model.md](data-model.md).
