# banco-alimentos Constitution

## Core Principles

### I. Arquitectura por features y dominio en español
La aplicación MUST organizarse por features (vertical slicing) bajo el paquete base
com.group6.BancoAlimentos. Donantes, Donacion, ItemDonacion, Producto, Remito,
DetalleRemito, Institucion y Factura MUST contener sus propios controller, service,
repository, modelo, DTOs y mapper. El código compartido MUST residir en Common
(excepciones y mappers), la configuración en Config y la autenticación en Security.
Una feature MUST NOT depender de clases internas de otra; la comunicación entre
features MUST realizarse mediante sus services. Los nombres de clases, métodos y campos
MUST estar en español y reflejar el dominio. Esta organización reduce el acoplamiento y
mantiene cada área funcional cohesionada.

### II. Reglas de negocio y errores estandarizados
Toda la lógica de negocio MUST residir en service; los controllers MUST limitarse a
recibir solicitudes, delegar y responder. Las reglas de negocio MUST validarse en
service, incluyendo deudas, ítems duplicados o vencidos y ajustes de stock. El sistema
MUST impedir que un producto vencido se entregue o se cargue a un remito. Cuando una
regla de negocio se incumpla, service MUST lanzar ReglaNegocioException y el sistema
MUST responder HTTP 422. GlobalExceptionHandler MUST manejar los errores globalmente y
devolver siempre el formato de respuesta estandarizado. Los controllers MUST exponer
DTOs, nunca entidades JPA, para preservar la separación entre API y persistencia.

### III. Seguridad y validación en el borde
La autenticación MUST usar JWT mediante jjwt y Spring Security MUST aplicar autorización
por roles y permisos. Los roles del proyecto MUST incluir user_deposito,
user_institucional y user_tesoreria. Cada endpoint MUST declarar explícitamente los
roles autorizados; los endpoints sin acceso por esos roles MUST declarar explícitamente
su política de acceso público o de autenticación requerida. Toda entrada MUST validarse
en el borde con Bean Validation en los DTOs y @Valid. Ninguna clave JWT, contraseña de
base de datos u otra credencial MUST guardarse en el repositorio; los secretos MUST
leerse de variables de entorno o archivos ignorados por Git.

### IV. Integridad y auditoría
Hibernate Envers MUST auditar los cambios de todas las entidades de negocio. Toda
regla crítica de integridad MUST aplicarse en service y contar con tests; en particular,
un producto vencido MUST NOT entregarse ni cargarse a un remito. Todo listado MUST
permitir filtrado por los datos propios de su entidad, y los listados principales MUST
usar paginación con Pageable. Estas restricciones protegen la integridad del inventario
y permiten rastrear los cambios relevantes.

### V. Testing, documentación y estilo
Toda regla de negocio MUST tener tests con JUnit 5 y Mockito, escritos antes o junto
con su implementación. Los endpoints MUST tener tests de integración con H2. Una tarea
MUST NOT considerarse terminada si los tests aplicables no pasan. Cada endpoint MUST
documentarse en OpenAPI mediante Springdoc. El código MUST priorizar claridad sobre
ingenio, usar nombres descriptivos y métodos breves, y limitar los comentarios a
explicar el porqué de decisiones no evidentes. Cada tarea MUST producir un commit
pequeño, descriptivo y escrito en español.

## Stack y entorno

El stack MUST usar Java 21, Spring Boot con Web MVC, Data JPA, Validation y Security, y
Maven ejecutado mediante el wrapper mvnw. MySQL MUST ser la base principal y H2 MUST
reservarse para pruebas. El proyecto MUST usar Lombok para reducir código repetitivo y
ModelMapper para convertir entre entidades y DTOs. Docker y Docker Compose MUST permitir
levantar el entorno. La inclusión de dependencias o abstracciones MUST justificarse por
una necesidad concreta; simplicidad MUST prevalecer cuando no exista una necesidad
probada.

## Organización de features y flujo de desarrollo

Cada controller MUST exponer sus endpoints bajo /api/... y devolver DTOs, nunca entidades
JPA. Los listados principales MUST aceptar paginación con Pageable y filtros por los
datos propios de la entidad. Los cambios MUST respetar los límites entre features y
canalizar la comunicación a través de los services correspondientes. Las credenciales
locales MUST residir únicamente en variables de entorno o archivos ignorados por Git.
El trabajo del equipo MUST cumplir los requisitos de testing, documentación y commits
establecidos en esta constitución antes de considerar una tarea finalizada.

## Governance

Esta constitución prevalece sobre prácticas incompatibles del proyecto. Toda enmienda
MUST documentar su motivo y alcance, actualizar este archivo en el mismo cambio y
someterse a revisión y aprobación de los mantenedores del proyecto. La revisión MUST
verificar compatibilidad con el dominio, la arquitectura y los requisitos de seguridad,
integridad y calidad establecidos aquí.

El versionado MUST seguir SemVer: MAJOR para eliminar o redefinir de forma incompatible
un principio, MINOR para agregar principios o ampliar materialmente las obligaciones,
y PATCH para aclaraciones sin cambio semántico. Cada enmienda MUST actualizar la fecha
de última modificación y registrar la razón de la nueva versión.

Las revisiones de código y la finalización de cada tarea MUST verificar el cumplimiento
de esta constitución. Los incumplimientos MUST corregirse antes de integrar o completar
el trabajo; cualquier excepción requiere una enmienda aprobada a esta constitución.

**Version**: 2.0.0 | **Ratified**: 2026-10-26 | **Last Amended**: 2026-10-26
