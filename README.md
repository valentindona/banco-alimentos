# Banco de Alimentos

API REST para administrar las operaciones de un banco de alimentos: donantes y donaciones, productos y stock, instituciones beneficiarias, remitos de entrega y consultas relacionadas. La aplicación está construida con Java 21 y Spring Boot; usa MySQL para ejecución normal y H2 para las pruebas automatizadas.

## Requisitos

- Java 21.
- MySQL 8.4 disponible localmente, o Docker Compose si se desea levantar la base incluida.
- Maven Wrapper incluido en el repositorio (`mvnw.cmd` para Windows y `mvnw` para Linux/macOS).

## Ejecutar pruebas con H2

H2 se usa como base en memoria para el perfil `test`; no hace falta levantar MySQL ni Docker para ejecutar las pruebas:

```powershell
.\mvnw.cmd test
```

Para ejecutar un grupo focalizado de pruebas:

```powershell
.\mvnw.cmd "-Dtest=ConsultaInstitucionServiceTest,InstitucionRepositoryTest,InstitucionServiceTest,InstitucionControllerIntegracionTest,InstitucionEstadoPagoIntegracionTest,InstitucionConsultaIntegracionTest,AutorizacionEndpointTest,ContratoInstitucionesOpenApiTest" test
```

La configuración H2 y los datos de prueba están en `src/test/resources/`. H2 tiene alcance de prueba en Maven: el modo normal de la aplicación usa MySQL.

## Ejecutar con MySQL

La aplicación aplica las migraciones Flyway al iniciar y valida el esquema con JPA. Se puede usar una instancia de MySQL 8.4 ya disponible o levantar la base definida en `docker-compose.yml`.

Para usar Docker Compose, crear un archivo `.env` en la raíz (está ignorado por Git) con las variables requeridas por el servicio MySQL:

```dotenv
MYSQL_DATABASE=banco_alimentos
MYSQL_USER=usuario_local
MYSQL_PASSWORD=contraseña_local
MYSQL_ROOT_PASSWORD=contraseña_root_local
```

Luego iniciar la base:

```powershell
docker compose up -d mysql
```

Configurar en la sesión de PowerShell las variables que necesita la aplicación. `DB_USERNAME` y `DB_PASSWORD` deben coincidir con `MYSQL_USER` y `MYSQL_PASSWORD` de la base; `JWT_SECRET` debe tener al menos 32 bytes:

```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/banco_alimentos"
$env:DB_USERNAME = "usuario_local"
$env:DB_PASSWORD = "contraseña_local"
$env:JWT_SECRET = "<secreto aleatorio de al menos 32 bytes>"
```

Para habilitar el perfil local, que crea el usuario inicial de depósito, definir también:

```powershell
$env:BOOTSTRAP_DEPOSITO_USUARIO = "deposito"
$env:BOOTSTRAP_DEPOSITO_CLAVE = "<contraseña de entre 12 y 128 caracteres>"
```

Iniciar la API:

```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=local"
```

No guardar claves, secretos ni credenciales reales en el repositorio. La configuración principal está en `src/main/resources/application.yml`; los valores locales y el alta del usuario inicial se configuran en `application-local.yml`.

## Estructura del código

El código Java se encuentra bajo `src/main/java/com/group6/BancoAlimentos/` y se organiza por funcionalidades:

- `donante`, `donacion`, `itemdonacion`: gestión de donantes, recepción de donaciones y sus ítems.
- `producto`, `stock`: catálogo de productos y existencias.
- `institucion`: instituciones beneficiarias y sus estados de pago.
- `remito`, `detalleremito`: preparación, consulta y confirmación de entregas.
- `factura`: consultas y comprobantes relacionados con recepciones.
- `notificacion`: notificaciones operativas.
- `usuario`: usuarios, autenticación de credenciales y sus roles de dominio.
- `Common`: componentes compartidos, como respuestas, excepciones y paginación.
- `Config`: configuración general y propiedades de la aplicación.
- `Security`: autenticación JWT, filtros y autorización.

Las funcionalidades siguen una organización por feature con controladores, servicios, repositorios, modelos, DTOs y mappers según corresponda. Los tests están en `src/test/java/` y los recursos de prueba en `src/test/resources/`.

## Roles

- `user_deposito`: administra donantes, productos y donaciones; gestiona stock y remitos; consulta instituciones y comprobantes.
- `user_institucional`: consulta, crea, modifica y da de baja lógicamente instituciones.
- `user_tesoreria`: consulta instituciones y actualiza su estado de pago.

Las rutas de la API están protegidas por JWT y sus permisos se definen en la configuración central de Spring Security. El detalle de operaciones, cuerpos y respuestas se encuentra en el contrato OpenAPI de cada feature.

## Documentación

- `specs/`: especificaciones, decisiones, modelos, contratos y guías de las funcionalidades.
- `.specify/memory/constitution.md`: principios de arquitectura, seguridad, validación, pruebas y estilo del proyecto.
- `src/main/resources/openapi.yaml`: contrato OpenAPI integrado para la API.
- `specs/003-gestion-instituciones-pagos/quickstart.md`: guía focalizada para probar la gestión de instituciones y estados de pago.
