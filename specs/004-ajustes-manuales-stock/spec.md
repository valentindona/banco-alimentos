# Feature Specification: Ajustes manuales de stock

**Feature Branch**: `[004-ajustes-manuales-stock]`

**Created**: 2026-10-27

**Status**: Draft

**Input**: User description: "Permitir al personal del depósito corregir cantidades de stock mal cargadas y dar de baja mercadería dañada, vencida o perdida, con registro de quién hizo el cambio, cuándo y por qué."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Corregir o dar de baja stock (Priority: P1)

El personal del depósito corrige la cantidad real de un lote o da de baja una cantidad por daño, vencimiento, pérdida u otro motivo. El sistema actualiza el stock y conserva un registro que permite explicar el cambio.

**Why this priority**: Mantener cantidades confiables y retirar mercadería que ya no está disponible es esencial para que las donaciones y los remitos operen con stock correcto.

**Independent Test**: Con una cuenta de depósito y un lote existente, realizar una corrección y una baja con motivos; comprobar la cantidad resultante y consultar los datos de auditoría de ambas operaciones.

**Acceptance Scenarios**:

1. **Given** un lote con stock, **When** depósito registra una baja válida con motivo, **Then** el stock disminuye exactamente la cantidad indicada y queda registrado el ajuste.
2. **Given** un lote con una cantidad cargada incorrectamente, **When** depósito fija la cantidad real con un motivo, **Then** el lote queda con esa cantidad y se conserva la cantidad anterior y la nueva.
3. **Given** un lote vencido o bloqueado por vencimiento, **When** depósito registra una baja válida, **Then** la baja se permite y el stock se actualiza sin habilitar el lote para una entrega.
4. **Given** un lote cuya baja consume todo el stock, **When** depósito confirma la baja, **Then** el lote queda en cero y el ajuste queda registrado.
5. **Given** un lote incluido en un remito en borrador, **When** depósito ajusta su stock, **Then** el ajuste se permite y, al intentar confirmar el remito, se vuelve a validar la disponibilidad; si ya no alcanza, la confirmación se rechaza sin modificar el remito confirmado ni el stock.

---

### User Story 2 - Consultar el historial de ajustes (Priority: P2)

El personal del depósito consulta los ajustes registrados para reconstruir qué cambió en cada lote, quién lo hizo, cuándo y por qué. Puede filtrar el historial por producto, vencimiento, fecha, usuario y tipo.

**Why this priority**: La trazabilidad permite verificar correcciones y bajas sin alterar ni perder evidencia histórica.

**Independent Test**: Registrar ajustes de más de un producto o fecha, consultar el historial con filtros individuales y combinados, y comprobar que cada resultado coincide con todos los filtros aplicados y muestra los datos de auditoría.

**Acceptance Scenarios**:

1. **Given** varios ajustes registrados, **When** depósito filtra por producto o fecha, **Then** el listado contiene solo los ajustes que coinciden con el filtro.
2. **Given** filtros de producto, vencimiento, fecha, usuario y tipo, **When** depósito aplica uno o varios filtros, **Then** cada resultado cumple todos los criterios informados.
3. **Given** filtros sin coincidencias, **When** depósito consulta el historial, **Then** obtiene un listado vacío y comprensible.
4. **Given** un usuario que no tiene rol de depósito, **When** intenta registrar o consultar ajustes, **Then** el acceso se deniega y no se modifica el stock ni el historial.

### Edge Cases

- Un motivo ausente, vacío o compuesto solo por espacios se rechaza.
- Una baja con cantidad cero, negativa o mayor al stock disponible se rechaza y no cambia el stock.
- Una corrección a una cantidad negativa se rechaza; fijar el lote en cero es válido.
- El producto o el lote (producto y vencimiento) inexistente se rechaza sin crear registros parciales.
- Los lotes vencidos o bloqueados por vencimiento pueden darse de baja aunque no estén disponibles para una entrega.
- Si dos usuarios ajustan el mismo lote a la vez, cada operación se resuelve respecto del stock vigente al registrarse; ninguna puede producir stock negativo y cada historial refleja las cantidades efectivamente anterior y nueva.
- Un ajuste puede dejar sin stock suficiente a un remito en borrador; este no reserva stock y debe volver a validar disponibilidad al confirmarse.
- Un ajuste no modifica donaciones, remitos ni facturas ya confirmados.
- Los ajustes registrados no se editan ni eliminan; un error se corrige con un nuevo ajuste.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir exclusivamente al rol de depósito registrar correcciones de cantidad y bajas de stock.
- **FR-002**: Cada ajuste MUST identificar un lote mediante el producto y su vencimiento; el sistema MUST rechazar productos o lotes inexistentes.
- **FR-003**: Una corrección MUST fijar la cantidad del lote en la cantidad real informada, expresada en la unidad de stock existente, y MUST rechazar cantidades negativas.
- **FR-004**: Una baja MUST descontar del lote la cantidad positiva informada por depósito y MUST rechazar cantidades superiores al stock disponible.
- **FR-005**: Todo ajuste MUST exigir un motivo de texto no vacío después de quitar espacios al inicio y al final.
- **FR-006**: El sistema MUST permitir bajas de lotes vencidos o bloqueados por vencimiento, sin hacerlos elegibles para entregas.
- **FR-007**: El sistema MUST permitir que una corrección o baja válida deje el lote en cero.
- **FR-008**: El sistema MUST actualizar el stock y registrar el ajuste como una única operación: si el ajuste se rechaza, no debe cambiar ni el stock ni el historial.
- **FR-009**: Si hay ajustes simultáneos sobre un mismo lote, cada uno MUST comprobar el stock vigente al aplicarse y MUST conservar las cantidades anterior y nueva efectivamente resultantes, sin permitir stock negativo.
- **FR-010**: Cada registro MUST conservar la fecha y hora, el usuario, el producto, el vencimiento del lote, el tipo de ajuste, la cantidad anterior, la cantidad nueva y el motivo.
- **FR-011**: Una vez registrado, un ajuste MUST ser inmutable y MUST NOT poder editarse ni eliminarse. Un error MUST corregirse registrando otro ajuste.
- **FR-012**: El rol de depósito MUST poder consultar el historial de ajustes y filtrarlo por producto, vencimiento, fecha del ajuste, usuario y tipo de ajuste. Al combinar filtros, el resultado MUST cumplir todos los criterios.
- **FR-013**: El sistema MUST denegar el registro y la consulta de ajustes a usuarios sin rol de depósito, sin alterar stock ni historial.
- **FR-014**: Los ajustes MUST NOT modificar donaciones, remitos ni facturas ya confirmados.
- **FR-015**: Un remito en borrador MUST NOT reservar stock frente a un ajuste. Al confirmarlo, el sistema MUST volver a validar la disponibilidad; si el stock resulta insuficiente, MUST rechazar la confirmación y mantener el remito sin confirmar.

### Key Entities *(include if feature involves data)*

- **Lote de stock**: Cantidad disponible de un producto para un vencimiento determinado. El ajuste opera sobre esta combinación y no convierte unidades.
- **Ajuste de stock**: Registro inmutable de una corrección (fijar la cantidad real) o una baja (descontar una cantidad), con autor, fecha y hora, lote, cantidades anterior y nueva, y motivo.
- **Usuario de depósito**: Usuario con el rol autorizado para registrar y consultar ajustes.
- **Remito en borrador**: Remito no confirmado que no reserva stock y cuya disponibilidad debe validarse al confirmarse.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100 % de las bajas válidas cambia el stock del lote exactamente por la cantidad solicitada y deja un registro consultable con todos los datos de auditoría requeridos.
- **SC-002**: El 100 % de los intentos inválidos —incluidos motivos vacíos, cantidades negativas o bajas superiores al stock— se rechaza sin cambios en el stock ni en el historial.
- **SC-003**: El 100 % de las bajas válidas de lotes vencidos o bloqueados por vencimiento puede completarse, y esos lotes no quedan disponibles para entregas por haber sido ajustados.
- **SC-004**: En pruebas de aceptación, el personal de depósito puede completar una corrección o una baja y verificar su registro en menos de 2 minutos por operación.
- **SC-005**: En el 100 % de las consultas de historial, los resultados cumplen todos los filtros seleccionados y muestran los datos necesarios para identificar quién cambió qué, cuándo y por qué.
- **SC-006**: El 100 % de los intentos de usuarios sin rol de depósito para registrar o consultar ajustes es denegado sin cambios en los datos.

## Assumptions

- Los ajustes se expresan en la unidad ya usada por el stock del lote; no se convierten unidades.
- El motivo es texto libre obligatorio. Para una baja, puede describir mercadería dañada, vencida, perdida u otra causa.
- La fecha y hora del registro y el usuario se obtienen de la operación autenticada, no de valores ingresados por quien ajusta.
- Los ajustes solo aplican a lotes existentes; esta función no crea productos ni lotes.
- Los lotes en cero permanecen en el historial y pueden consultarse.
- **Recomendación para planificación sobre remitos en borrador**: permitir el ajuste y revalidar el stock al confirmar el remito. Si la cantidad ya no alcanza, rechazar la confirmación; así se permite corregir el inventario sin tratar un borrador como una reserva, manteniendo íntegra la regla de disponibilidad.
