# Feature Specification: Gestión de instituciones y estados de pago

**Feature Branch**: `[003-gestion-instituciones-pagos]`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Gestión de instituciones beneficiarias del banco de alimentos y del estado de pago de su cuota, con acceso diferenciado para los roles institucional, tesorería y depósito."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Administrar instituciones (Priority: P1)

El personal institucional consulta las instituciones beneficiarias y puede dar de alta, modificar o dar de baja sus datos. Así mantiene actualizado el padrón que utilizan las áreas del banco.

**Why this priority**: Mantener un padrón correcto es la base para que las demás áreas consulten información confiable.

**Independent Test**: Con una cuenta institucional, crear una institución con datos válidos y comprobar que aparece en el listado; luego modificarla y darla de baja, verificando los cambios y que los remitos previos continúan disponibles.

**Acceptance Scenarios**:

1. **Given** un usuario institucional y datos válidos de una institución, **When** da de alta la institución, **Then** esta queda registrada y aparece en los listados.
2. **Given** una institución existente, **When** el usuario institucional modifica sus datos, **Then** la información actualizada se muestra en las consultas posteriores.
3. **Given** una institución con remitos previos, **When** el usuario institucional la da de baja, **Then** la institución deja de estar disponible como institución activa y los remitos históricos se conservan.
4. **Given** datos incompletos o un nombre que ya está registrado, **When** el usuario intenta dar de alta la institución, **Then** el alta se rechaza y se informa qué dato debe corregirse.

### User Story 2 - Consultar y actualizar estados de pago (Priority: P1)

Tesorería consulta y filtra instituciones, y actualiza el estado de pago de la cuota para reflejar si están al día, en deuda o son particulares.

**Why this priority**: Tesorería necesita mantener visible el estado de pago vigente sin convertir la función en un sistema de cobro.

**Independent Test**: Con una cuenta de tesorería, filtrar una institución, cambiar su estado y comprobar que el nuevo estado aparece en las consultas y listados.

**Acceptance Scenarios**:

1. **Given** un usuario de tesorería y una institución existente, **When** cambia el estado de pago, **Then** el nuevo estado se muestra en las consultas posteriores.
2. **Given** una institución, **When** tesorería vuelve a seleccionar su estado actual, **Then** la institución conserva ese estado y la operación no produce un error.
3. **Given** una institución en deuda, **When** se registra un remito a su nombre, **Then** el remito puede registrarse sin restricciones por el estado de pago.

### User Story 3 - Consultar instituciones por área (Priority: P2)

Depósito consulta y filtra instituciones por nombre, tipo o estado de pago para encontrar la información necesaria para su trabajo, sin poder modificarla.

**Why this priority**: La consulta ayuda a depósito a identificar instituciones, mientras que las modificaciones quedan limitadas a los roles responsables.

**Independent Test**: Con una cuenta de depósito, filtrar instituciones y verificar que los resultados coinciden con los criterios; intentar modificar una institución o su estado y confirmar que el acceso es denegado.

**Acceptance Scenarios**:

1. **Given** un usuario autorizado de tesorería o depósito y filtros de nombre, tipo o estado de pago, **When** consulta el listado, **Then** se muestran únicamente las instituciones que cumplen todos los filtros indicados.
2. **Given** un filtro que no coincide con ninguna institución, **When** el usuario consulta el listado, **Then** se informa que no hay resultados sin presentar instituciones que no coincidan.
3. **Given** un usuario de depósito, **When** intenta crear, modificar o dar de baja una institución, o cambiar su estado de pago, **Then** la operación es denegada y los datos permanecen sin cambios.
4. **Given** un usuario que no tiene el rol permitido para una operación, **When** intenta realizarla, **Then** el acceso es denegado y los datos permanecen sin cambios.

### Edge Cases

- Un alta con datos requeridos incompletos o con un nombre ya existente se rechaza con una indicación del problema.
- Seleccionar el estado de pago que ya tiene una institución no cambia el resultado ni genera un error.
- Dar de baja una institución con remitos no elimina ni altera esos remitos históricos.
- Un usuario sin el rol correspondiente no puede modificar datos de instituciones ni estados de pago.
- Una búsqueda sin coincidencias presenta un resultado vacío y comprensible.
- El estado «particular» identifica una condición de la institución y no debe tratarse como deuda.
- Una institución en deuda continúa habilitada para recibir entregas y registrar remitos.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir al rol institucional consultar, dar de alta, modificar y dar de baja instituciones beneficiarias.
- **FR-002**: El sistema MUST validar los datos requeridos de una institución y rechazar un alta incompleta o cuyo nombre ya exista, informando el motivo del rechazo.
- **FR-003**: El sistema MUST asociar a cada institución un único estado de pago vigente, con los valores «al día», «en deuda» y «particular».
- **FR-004**: El estado «particular» MUST representar una condición particular de la institución y MUST NOT interpretarse como deuda.
- **FR-005**: El estado de pago MUST reflejar únicamente la situación de pago de la cuota institucional; esta función MUST NOT registrar pagos, importes ni historial de pagos.
- **FR-006**: El sistema MUST permitir a tesorería consultar y filtrar instituciones y modificar el estado de pago vigente.
- **FR-007**: El sistema MUST permitir a depósito consultar y filtrar instituciones, sin permitirle modificar sus datos ni el estado de pago.
- **FR-008**: Los listados MUST poder filtrarse por nombre, tipo de institución y estado de pago; si se aplican varios criterios, los resultados MUST cumplirlos todos.
- **FR-009**: El sistema MUST comunicar claramente cuando una consulta no encuentre instituciones coincidentes.
- **FR-010**: El sistema MUST denegar operaciones de consulta o modificación que el rol del usuario no tenga autorizadas, sin alterar los datos.
- **FR-011**: Dar de baja una institución MUST conservar los remitos históricos asociados y sus datos históricos.
- **FR-012**: El estado «en deuda» MUST NOT impedir que una institución reciba entregas ni que se registren remitos a su nombre.
- **FR-013**: Cambiar el estado de pago al mismo valor vigente MUST dejar la institución en ese estado sin producir un error.
- **FR-014**: La gestión de instituciones y estados de pago MUST NOT cambiar el funcionamiento existente de los remitos.

### Key Entities *(include if feature involves data)*

- **Institución**: Organización beneficiaria con los datos propios ya utilizados por el sistema, un tipo y un estado de pago vigente. Puede estar activa o dada de baja; los remitos históricos asociados se conservan.
- **Estado de pago**: Situación vigente de la cuota de una institución, con los valores «al día», «en deuda» o «particular». No representa importes ni movimientos de pago.
- **Remito**: Registro existente de una entrega asociada a una institución. Esta función no modifica su gestión y sus registros históricos se conservan al dar de baja una institución.
- **Rol de usuario**: Permiso que determina las operaciones disponibles: institucional administra instituciones; tesorería consulta y modifica estados de pago; depósito consulta instituciones.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las altas con datos válidos realizadas por usuarios institucionales aparecen en los listados de instituciones.
- **SC-002**: El 100 % de las instituciones consultadas con filtros por nombre, tipo o estado cumplen todos los filtros aplicados; las consultas sin coincidencias muestran un listado vacío.
- **SC-003**: El 100 % de los cambios de estado válidos realizados por tesorería se reflejan en las consultas posteriores, incluidos los cambios al mismo valor.
- **SC-004**: El 100 % de los intentos de modificación realizados por usuarios sin el rol autorizado son denegados y dejan los datos sin cambios.
- **SC-005**: El 100 % de los remitos históricos asociados a instituciones dadas de baja permanecen consultables, y las instituciones en deuda pueden seguir recibiendo entregas.
- **SC-006**: Al menos el 90 % de usuarios de prueba de los roles previstos pueden completar su tarea principal de consulta o gestión sin ayuda externa.

## Assumptions

- Se reutilizan los datos actuales de institución que el sistema ya emplea para remitos; el alcance no define campos institucionales nuevos aparte del tipo y el estado de pago requeridos para esta función.
- Los permisos se asignan según los roles existentes: institucional para el ABM, tesorería para la consulta y actualización del estado de pago, y depósito para consulta.
- Los nombres de instituciones deben ser únicos; un intento de alta con un nombre ya registrado se rechaza.
- La baja conserva la información necesaria para consultar los remitos históricos, sin eliminar ni alterar dichos remitos.
- La función administra solo el estado vigente; no incorpora cobros, montos, historial ni notificaciones.
- Los listados reflejan los cambios de datos y estado una vez confirmada cada operación.
