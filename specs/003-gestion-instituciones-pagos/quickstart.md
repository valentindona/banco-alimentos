# Quickstart: Gestión de instituciones y estados de pago

## Prerrequisitos

- Java 21 y el wrapper Maven del repositorio.
- Configuración de prueba existente en `src/test/resources/application-test.yml`; las pruebas de integración usan H2, Flyway y el perfil `test`.

## Validación automatizada

Desde la raíz del repositorio:

```powershell
.\mvnw.cmd "-Dtest=ConsultaInstitucionServiceTest,InstitucionRepositoryTest,InstitucionServiceTest,AutorizacionEndpointTest,ContratoInstitucionesOpenApiTest" test
```

El selector puede ajustarse a los nombres definitivos de las nuevas clases de prueba durante la implementación. Los resultados esperados son:

- Migraciones aplicadas a H2 y esquema validado por JPA.
- Reglas de nombre duplicado, validación de datos, baja lógica y cambio de estado verificadas.
- Filtros de nombre, tipo y estado combinados correctamente y paginación limitada como en el helper común.
- Usuarios de depósito, tesorería e institucional autorizados solo para las operaciones definidas en el contrato.
- Los remitos continúan aceptando instituciones en deuda y los datos históricos se mantienen al dar de baja.

Para ejecutar toda la suite:

```powershell
.\mvnw.cmd test
```

## Escenarios manuales de API

Autenticarse con una cuenta de cada rol y utilizar el token Bearer correspondiente.

1. Como `user_institucional`, crear una institución con nombre, domicilio y tipo. Esperar HTTP 201 y una respuesta con `estadoPago: AL_DIA` (supuesto) y `activa: true`.
2. Como `user_deposito`, listar instituciones con `nombre`, `tipo`, `estadoPago`, `pagina` y `tamano`; comprobar que todos los filtros se combinan y que la respuesta usa la envoltura paginada común.
3. Como `user_tesoreria`, cambiar el estado a `EN_DEUDA` y consultar la institución para comprobar el valor actualizado. Repetir el mismo cambio y esperar éxito sin alterar el estado.
4. Como `user_deposito`, intentar crear o actualizar una institución, darla de baja o cambiar su estado; esperar HTTP 403 y verificar que no hubo cambios.
5. Crear un remito para una institución en estado `EN_DEUDA`; esperar que las reglas normales de remito sigan aplicándose y que la deuda no lo bloquee.
6. Como `user_institucional`, dar de baja una institución con remitos y comprobar que el recurso operativo queda inactivo mientras sus remitos siguen consultables.

Los paths, cuerpos, parámetros, roles y respuestas están detallados en [contracts/openapi.yaml](contracts/openapi.yaml); los campos y sus reglas están en [data-model.md](data-model.md).
