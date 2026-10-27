# Quickstart: validar entrega de mercadería con remitos

Esta guía valida el flujo de extremo a extremo desde una base limpia. Los comandos
presuponen Windows PowerShell para el wrapper Maven; en otros sistemas usar `./mvnw`.

## Prerrequisitos

- Java 21 instalado.
- Docker Desktop con Docker Compose.
- Variables de entorno para MySQL y JWT definidas localmente; no guardar secretos en Git.

## Arranque local

1. Configurar las variables requeridas por `docker-compose.yml` y la aplicación, entre
   ellas las credenciales de MySQL y `JWT_SECRET`.
2. Iniciar la base de datos:

   ```powershell
   docker compose up -d mysql
   ```

3. Ejecutar las migraciones y el servicio:

   ```powershell
   .\mvnw.cmd spring-boot:run
   ```

4. Verificar que OpenAPI esté disponible en `/swagger-ui/index.html` y que la salud de
   la aplicación responda correctamente.
5. Para el primer arranque local, definir `BOOTSTRAP_DEPOSITO_USUARIO` y
   `BOOTSTRAP_DEPOSITO_CLAVE` (12-128 caracteres) además de `JWT_SECRET`. El perfil
   `local` crea esa cuenta de depósito solo si todavía no existe; no reemplaza claves
   de cuentas existentes. Luego, un usuario de depósito puede provisionar las demás
   cuentas mediante `POST /api/usuarios`. No guardar estos valores en Git.

## Escenarios de validación

1. **Inicio de sesión y permisos**: iniciar sesión como `user_deposito`, obtener el
   token y crear un borrador. Repetir el intento de registro con tesorería e institucional;
   ambos deben recibir `403`.
2. **Confirmación correcta**: cargar producto, institución y stock de prueba con una
   fecha de vencimiento futura; crear borrador, añadir una línea y confirmar. Consultar
   stock antes/después y verificar que disminuyó exactamente la cantidad entregada.
3. **Producto vencido**: intentar agregar y luego confirmar una línea cuya existencia
   vence hoy o antes. Debe recibirse `422`; el remito y todo el stock deben permanecer
   sin cambios.
4. **Stock insuficiente/atomicidad**: crear un remito con dos líneas, hacer insuficiente
   una existencia y confirmar. La operación debe rechazarse y ninguna existencia debe
   descontarse.
5. **Vencimiento durante el borrador**: preparar un remito para una existencia que
   vencerá antes de confirmarlo. La confirmación debe volver a validar la fecha y
   rechazar la entrega.
6. **Avisos diarios**: ejecutar el service de proceso diario en pruebas con existencias
   que vencen en 3 y 4 días, hoy y en el pasado. Debe crear avisos para los 3 días y los
   vencidos, ninguno para los 4 días; ejecutarlo repetidamente no debe duplicarlos.
7. **Deuda y consultas**: confirmar una entrega a una institución `EN_DEUDA`; depósito
   consulta facturas e instituciones, tesorería consulta solo instituciones y cada
   usuario institucional solo consulta remitos de su propia institución.

## Validación automatizada

Ejecutar la suite:

```powershell
.\mvnw.cmd clean verify
```

El build debe terminar correctamente. Las pruebas unitarias cubren reglas del service;
las pruebas de integración usan H2 para autenticación, permisos, respuestas de error,
filtros y confirmación. Las condiciones de bloqueo pesimista deben verificarse además
con pruebas de integración que ejecuten MySQL, dado que H2 no reproduce todas las
garantías de concurrencia de InnoDB.

Validación ejecutada en Windows con `.\mvnw.cmd clean verify`: BUILD SUCCESS, 82
pruebas ejecutadas, 0 fallas y 0 errores. La única prueba omitida es
`ConfirmacionRemitoConcurrenciaTest`, condicionada a `MYSQL_TEST_URL`; sin esa variable
no se valida concurrencia sobre MySQL. Flyway aplicó las migraciones V1–V6 sobre el
esquema H2 limpio del perfil `test`. La validación MySQL y el arranque de Compose no se
ejecutaron porque requieren MySQL/Docker.

Para comprobar el arranque de la aplicación sin Docker ni una base MySQL, se puede usar
el perfil `test`, una base H2 en memoria y valores ficticios locales. No utilizar este
perfil ni esos valores en producción:

```powershell
$env:DB_URL = 'jdbc:h2:mem:banco-alimentos;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE'
$env:DB_USERNAME = 'sa'
$env:DB_PASSWORD = ''
$env:JWT_SECRET = 'solo-para-prueba-local-sin-uso-produccion-1234'
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=test" "-Dspring-boot.run.useTestClasspath=true" "-Dspring-boot.run.arguments=--server.port=18081"
```

La configuración local/Compose de producción requiere definir `DB_URL`, `DB_USERNAME`,
`DB_PASSWORD`, `JWT_SECRET` y las variables `MYSQL_*` indicadas en `docker-compose.yml`.
Mantener sus valores únicamente en el entorno local; los nombres de archivos sensibles
y credenciales de producción no deben incorporarse al repositorio.

Ver [data-model.md](./data-model.md) para relaciones y [contracts/openapi.yaml](./contracts/openapi.yaml)
para rutas, roles y esquemas de request/response.
