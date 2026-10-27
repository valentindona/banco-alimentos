# Frontend Angular - Banco de Alimentos Mar del Plata

## Estructura e identidad visual

- Angular standalone, TypeScript y SCSS viven en `frontend/`; la API Java permanece en la raíz.
- El panel usa navegación lateral y accesos rápidos por rol. Los listados se presentan como cards con acciones contextuales, formularios y estados de carga, error y vacío.
- Los estilos propios mantienen la identidad azul y verde menta sin agregar una biblioteca de componentes.

| Uso | Color |
| --- | --- |
| Fondo de superficies | Blanco #FFFFFF |
| Texto principal | Azul oscuro #0F2A44 |
| Navegación y acentos | Azul medio #1F5FB0 |
| Grandes áreas de marca | Azul #3A8DDE |
| Acciones y secciones pequeñas | Verde menta #3BD4AE |

Mantener contraste legible, por ejemplo texto azul oscuro sobre menta. El logo está pendiente; se usa una marca tipográfica temporal.

## Desarrollo

1. Iniciar la API Java en `localhost:8080` con la configuración del README raíz.
2. Desde `frontend/`, ejecutar `npm install` si hace falta y `npm start`.
3. Abrir `localhost:4200`. `proxy.conf.json` reenvía `/api` a `localhost:8080`.

En producción publicar el build estático y configurar un reverse proxy `/api` hacia la API, más fallback a `index.html`. Se prefiere mismo origen mediante proxy; evitar CORS comodín y usar TLS.

## Sesión y seguridad

- `POST /api/auth/login` recibe `nombreUsuario` y `clave`; devuelve JWT.
- `GET /api/auth/yo` valida la sesión y devuelve identidad y rol.
- El token se guarda en `sessionStorage` y se envía como `Authorization: Bearer`. Un `401` cierra la sesión; un `403` informa permiso insuficiente. No hay refresh token implementado; reautenticar al vencer.
- El menú, el panel y las acciones se limitan según el rol. La API sigue siendo la autoridad de permisos y valida cada operación; la interfaz maneja explícitamente `401` y `403`.
- No guardar claves, tokens ni datos personales reales en el frontend. Usar HTTPS fuera de desarrollo.

## Módulos y acciones

- **USER_DEPOSITO:** productos y donantes (alta, listado, edición y baja lógica); stock (consulta); donaciones (registrar recepción con comprobante automático); remitos (crear, editar y eliminar borradores, editar/agregar/quitar artículos y confirmar); ajustes (registrar, consultar, editar y eliminar si no hubo movimientos posteriores del lote); facturas (consulta y generación/reparación idempotente del comprobante de una donación); notificaciones (consulta); instituciones (consulta) y alta de usuarios.
- **USER_INSTITUCIONAL:** instituciones (consulta, alta, edición y baja lógica).
- **USER_TESORERIA:** instituciones (consulta y cambio de estado de pago).

Los listados paginados muestran 20 registros por página. Los formularios de donaciones, remitos, ajustes y facturas permiten seleccionar las referencias por nombre y muestran también su ID; los lotes incluyen producto, vencimiento y cantidad disponible. La generación de un comprobante devuelve el existente si ya fue emitido. Las donaciones confirmadas, facturas emitidas, stock, usuarios y notificaciones no tienen edición o eliminación genérica: sus cambios tienen efectos contables/de inventario o la API no expone esas operaciones. Los remitos solo se pueden cambiar mientras están en borrador. Editar o eliminar un ajuste requiere que el historial auditado del lote no tenga cambios posteriores, y la baja restaura el saldo previo. Ver `src/main/resources/openapi.yaml` y los contratos bajo `specs/*/contracts/` para DTOs y paginación.

## Próximos pasos

1. Validar visualmente densidad, formularios y recorridos con el equipo; integrar el logo cuando exista.
2. Incorporar búsqueda y filtros de negocio en los listados según las necesidades del equipo.
3. Añadir pruebas e2e; verificar navegación por teclado y contraste.
4. Acordar dominio, TLS, proxy, entorno API, despliegue y rollback antes de producción.

Figma es opcional para validar recorridos y componentes. No compartir secretos ni datos sensibles con herramientas externas.
