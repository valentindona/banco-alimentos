# Banco de Alimentos Mar del Plata - Frontend

Aplicación interna de escritorio desarrollada en Angular. El proyecto vive en frontend y la API Java permanece en la raíz.

## Requisitos y desarrollo

- Node.js LTS y npm.
- API iniciada en http://localhost:8080; configurar base, JWT y cuentas locales según el README raíz.
- Desde esta carpeta: npm install si hace falta, luego npm start; abrir http://localhost:4200.

El proxy de desarrollo (proxy.conf.json) reenvía /api al backend local. El navegador usa rutas relativas; no hace falta CORS abierto para desarrollo.

## Estado inicial

Incluye shell de escritorio, login JWT, consulta de identidad, menú filtrado por rol y consulta inicial de endpoints GET. La respuesta se muestra temporalmente en JSON para verificar integración. Formularios CRUD, filtros, tablas, paginación y flujos completos siguen pendientes; consultar docs/frontend.md.

## Producción

Publicar como sitio estático separado. Configurar reverse proxy /api al backend y fallback de navegación a index.html. Usar HTTPS; no habilitar CORS para cualquier origen ni incluir contraseñas o tokens en el repositorio.
