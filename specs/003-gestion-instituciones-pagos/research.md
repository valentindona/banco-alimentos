# Research: Gestión de instituciones y estados de pago

## Decisiones

### Reutilizar la feature de Institución existente

- **Decision**: Completar `com.group6.BancoAlimentos.institucion`; conservar `Institucion`, `InstitucionRepository`, `InstitucionController`, `ConsultaInstitucionService` y `EstadoPago`.
- **Rationale**: La feature ya sirve como referencia de institución para remitos, expone una lista paginada y ya limita el estado a `AL_DIA`, `EN_DEUDA` y `PARTICULAR`. Mantenerla evita duplicar el modelo y cambiar el funcionamiento de remitos.
- **Alternatives considered**: Crear otro agregado o modelo de institución; descartado por duplicar los datos existentes y aumentar el riesgo de inconsistencias con remitos y usuarios.

### Agregar tipo de institución

- **Decision**: Incorporar `tipo` como texto de hasta 80 caracteres, filtrable por coincidencia exacta tras quitar espacios exteriores. La entidad y la columna principal permiten `null` para mantener compatibilidad con instituciones históricas; las solicitudes de alta/modificación conservan la validación obligatoria del contrato. El usuario confirmó texto libre sin catálogo cerrado. Agregarlo a la entidad y a su tabla de auditoría mediante una migración Flyway V8, con backfill `Sin especificar` para datos existentes.
- **Rationale**: El filtro por tipo forma parte del requisito, pero el esquema y DTO actuales no contienen ese dato. No existe un catálogo de tipos en la solicitud ni en la entidad actual, por lo que el usuario confirmó texto libre como opción de menor alcance. El backfill permite migrar registros sin inventar sus tipos originales.
- **Alternatives considered**: Omitir el campo y el filtro; descartado porque no cumpliría el criterio de búsqueda por tipo. Crear un catálogo de tipos; descartado porque agregaría administración y decisiones de negocio no pedidas.

### Separar permisos de consulta, padrón y estado de pago

- **Decision**: Autorizar GET a los tres roles previstos; POST, PUT y DELETE a `user_institucional` para administrar todas las instituciones; y PATCH de estado de pago únicamente a `user_tesoreria`. El usuario confirmó el alcance global del rol institucional.
- **Rationale**: La configuración actual deja la consulta de instituciones a depósito y tesorería y no declara permisos de escritura para este recurso. Las reglas explícitas responden a la responsabilidad indicada y evitan que la gestión de datos institucionales permita modificar el estado de pago.
- **Alternatives considered**: Permitir a depósito modificar el recurso por analogía con otras features; descartado porque contradice los requisitos. Permitir al rol institucional modificar el estado; descartado porque ese cambio corresponde a tesorería.

### Mantener la baja lógica y la auditoría

- **Decision**: Desactivar instituciones con `activa = false`, excluirlas de listados y consultas operativas y mantener sus registros, vínculos y revisiones Envers.
- **Rationale**: `Institucion` ya tiene el campo `activa` y los remitos tienen una clave foránea a la institución. La baja física no preservaría los vínculos históricos.
- **Alternatives considered**: Borrado físico con reescritura de claves foráneas o snapshots adicionales; descartado porque modifica el comportamiento y el modelo de remitos sin ser necesario.

### Validar nombres duplicados en el servicio

- **Decision**: Validar por nombre normalizado sin distinguir mayúsculas/minúsculas al crear o cambiar el nombre, y rechazar un duplicado existente con `ReglaNegocioException`.
- **Rationale**: El requisito requiere rechazar nombres existentes y la feature existente centraliza reglas de negocio en el servicio. No se requiere modificar ni reconciliar registros previos.
- **Alternatives considered**: Alterar en migración el nombre o eliminar duplicados existentes; descartado porque alteraría datos ajenos al cambio solicitado. Agregar dependencia o abstracción extra; no hace falta.

### Estado inicial de pago

- **Decision**: Inicializar en `AL_DIA` toda institución nueva; el estado no será parte del DTO de alta/modificación y tesorería será quien lo actualice. El usuario confirmó el valor inicial.
- **Rationale**: Cada institución debe tener un estado, y la especificación atribuye a tesorería la modificación de ese estado. El valor confirmado permite que el alta institucional no usurpe esa responsabilidad.
- **Alternatives considered**: Crear una institución sin estado; descartado porque el campo es requerido y no nulo. Permitir elegir el estado en el ABM institucional; descartado por la separación explícita de roles.

## Patrones existentes confirmados

- Paginación común: `Paginacion.crear` limita páginas a 100 y normaliza índice y tamaño; `PaginaRespuesta` define el formato de salida.
- Mapeo: `ConfiguracionGeneral` ya publica un `ModelMapper`; la feature agregará su mapper y lo reutilizará para conversiones entre entidad y DTO.
- Error de negocio: `ReglaNegocioException` es manejada por `GlobalExceptionHandler` con HTTP 422.
- Persistencia: MySQL es el motor principal y H2 de perfil `test` ejecuta Flyway con modo MySQL y validación del esquema.
- Auditoría: `Institucion` ya está anotada con Envers y usa `instituciones_aud`.
- API: los recursos usan DTOs, `@Valid`, rutas `/api/...` y OpenAPI en `src/main/resources/openapi.yaml`.
- Permisos: Spring Security usa las autoridades con nombres de rol `user_deposito`, `user_institucional` y `user_tesoreria`; las rutas se configuran centralmente en `ConfiguracionSeguridad`.
