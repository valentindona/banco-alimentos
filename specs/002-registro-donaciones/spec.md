# Feature Specification: Registro de donaciones y comprobantes de recepción

**Feature Branch**: `002-registro-donaciones`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Permitir al personal del depósito registrar donaciones de alimentos recibidas, ingresar los productos y sus vencimientos al stock y generar automáticamente un comprobante de recepción asociado a cada donación."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar una donación y emitir su comprobante (Priority: P1)

Una persona de depósito registra una donación con fecha, donante y uno o más
productos recibidos. Para cada producto indica cantidad, unidad, tipo de alimento y
vencimiento; puede seleccionar un producto del catálogo para completar sus datos o
ingresarlos manualmente. Al confirmar, la donación queda registrada, se incorpora cada
cantidad al stock de su producto y vencimiento, y se genera un comprobante de recepción
con el detalle completo.

**Why this priority**: Este es el flujo de entrada de mercadería que abastece el stock
utilizado por las entregas y debe dejar constancia verificable de lo recibido.

**Independent Test**: Confirmar una donación válida con varios productos y vencimientos;
comprobar que cada cantidad aumenta exactamente en su combinación de producto y fecha,
y que existe un comprobante asociado que identifica la fecha, el donante y cada línea.
Intentar luego una donación con una línea inválida y comprobar que no se registra
ninguna parte de la operación.

**Acceptance Scenarios**:

1. **Given** un donante válido y productos con cantidades positivas y vencimientos
   futuros, **When** depósito confirma la donación, **Then** se registra la donación,
   aumenta el stock de cada producto y vencimiento exactamente según lo recibido y se
   genera un comprobante asociado con el detalle completo.
2. **Given** una donación con una línea inválida, **When** depósito intenta confirmarla,
   **Then** se rechaza la donación completa y no quedan cambios en donación, stock ni
   comprobantes.
3. **Given** dos líneas del mismo producto con vencimientos distintos, **When** depósito
   confirma la donación, **Then** cada cantidad se incorpora al lote correspondiente sin
   combinar ni reemplazar sus vencimientos.
4. **Given** un producto del catálogo, **When** depósito lo selecciona para una línea,
   **Then** se completan su tipo de alimento y unidad de medida, que pueden modificarse
   para esa línea sin alterar los datos del catálogo.
5. **Given** una donación confirmada, **When** depósito consulta el stock disponible
   para preparar un remito, **Then** las cantidades recibidas quedan disponibles con su
   vencimiento y se conservan las reglas existentes de vencimiento y entrega.

### User Story 2 - Administrar donantes y productos (Priority: P2)

El personal de depósito mantiene los datos de los donantes y el catálogo de productos
para agilizar el registro de futuras donaciones. Puede dar de alta, consultar,
modificar y dar de baja donantes; y consultar y administrar productos con su nombre,
tipo de alimento y unidad de medida. Cuando ingresa un producto manualmente para una
donación, sus datos quedan vinculados a un producto consultable para el inventario.

**Why this priority**: Los registros consistentes de donantes y productos reducen errores
en la recepción y hacen posible identificar cada artículo en stock y en los remitos.

**Independent Test**: Crear y consultar un donante y un producto; modificar sus datos;
dar de baja el donante y verificar que no pueda usarse en nuevas donaciones y que las
donaciones previas sigan identificando al donante.

**Acceptance Scenarios**:

1. **Given** datos completos de un donante, **When** depósito lo registra, modifica o
   consulta, **Then** se conserva y muestra la información actualizada.
2. **Given** un donante con donaciones registradas, **When** depósito lo da de baja,
   **Then** deja de estar disponible para nuevas donaciones y continúa visible en el
   historial asociado a sus donaciones previas.
3. **Given** el catálogo inicial, **When** depósito consulta sus productos, **Then**
   están disponibles al menos leche, harina y galletitas, cada uno con tipo de alimento
   y unidad de medida.
4. **Given** datos completos de un producto no existente, **When** depósito lo ingresa
   manualmente, **Then** puede utilizarlo en una donación y consultarlo luego como
   producto del inventario.

### User Story 3 - Consultar y filtrar donaciones y comprobantes (Priority: P3)

El personal de depósito consulta donantes, donaciones, productos y comprobantes, y
restringe cada listado usando los datos propios de la información: nombre del donante,
fecha de donación, producto y fecha de vencimiento, entre otros atributos relevantes.

**Why this priority**: La búsqueda por los datos de recepción permite rastrear el origen
de los alimentos y recuperar comprobantes sin interferir con el flujo de entrega.

**Independent Test**: Registrar varias donaciones de distintos donantes, fechas y
productos; aplicar cada filtro y verificar que solo aparezcan los registros que
coinciden y que las líneas de producto mantengan sus datos de vencimiento.

**Acceptance Scenarios**:

1. **Given** donaciones de varios donantes y fechas, **When** depósito filtra por
   donante o intervalo de fechas, **Then** recibe solo las donaciones coincidentes.
2. **Given** productos y líneas recibidas con distintos nombres y vencimientos,
   **When** depósito filtra por producto o vencimiento, **Then** recibe solo los
   productos o líneas que coinciden.
3. **Given** comprobantes asociados a donaciones, **When** depósito filtra por donante
   o fecha, **Then** cada resultado conserva su asociación y refleja los datos de la
   donación correspondiente.
4. **Given** una persona usuaria que no pertenece a depósito, **When** intenta crear o
   modificar donantes, productos o donaciones, confirmar recepciones o consultar
   comprobantes, **Then** se le deniega la operación y no se modifica información.

### Edge Cases

- Una donación sin productos, o con una cantidad cero o negativa, se rechaza completa.
- Un producto con fecha de vencimiento ausente, igual a hoy o anterior se rechaza; el
  stock no admite alimentos vencidos.
- El mismo producto puede aparecer más de una vez si las líneas corresponden a
  vencimientos distintos; cada línea incrementa solo el lote correspondiente.
- Un donante inexistente, dado de baja o sin nombre se rechaza para una nueva donación.
- Un producto manual debe incluir nombre, tipo de alimento y unidad de medida; los
  datos incompletos se rechazan.
- Modificar los datos precargados de un producto al completar una línea no debe cambiar
  el catálogo ni los datos históricos de otras donaciones.
- Un fallo al registrar cualquier línea, actualizar stock o generar el comprobante
  cancela la operación completa: no queda donación parcial, stock incrementado ni
  comprobante huérfano.
- Un usuario sin rol de depósito no puede realizar operaciones de escritura aunque
  conozca los identificadores de los registros.
- Dar de baja un donante no elimina ni altera donaciones o comprobantes ya registrados.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST permitir al personal de depósito dar de alta, consultar,
  modificar y dar de baja donantes.
- **FR-002**: El sistema MUST exigir nombre para un donante activo; sus datos de
  contacto adicionales pueden ser opcionales.
- **FR-003**: El sistema MUST permitir registrar una donación con fecha, donante activo
  y una o más líneas de productos recibidos.
- **FR-004**: Cada línea MUST identificar un producto, una cantidad positiva, unidad de
  medida, tipo de alimento y fecha de vencimiento.
- **FR-005**: El sistema MUST proporcionar un catálogo inicial que incluya leche,
  harina y galletitas, y que informe para cada producto su tipo de alimento y unidad de
  medida.
- **FR-006**: El sistema MUST permitir seleccionar un producto del catálogo y completar
  su tipo de alimento y unidad de medida; los cambios hechos para una línea de
  donación MUST NOT modificar el catálogo.
- **FR-007**: El sistema MUST permitir ingresar manualmente un producto que no esté en
  el catálogo, con nombre, tipo de alimento y unidad de medida, y asociarlo al
  inventario para futuras consultas y entregas.
- **FR-008**: El sistema MUST rechazar donaciones sin líneas, cantidades no positivas,
  líneas incompletas, donantes inexistentes o inactivos y productos sin vencimiento o
  con vencimiento igual a hoy o anterior.
- **FR-009**: Al confirmar una donación válida, el sistema MUST incrementar el stock
  exactamente por producto y fecha de vencimiento, sin combinar existencias de
  vencimientos distintos.
- **FR-010**: Cada donación confirmada MUST generar automáticamente un único comprobante
  de recepción asociado a ella, con fecha, donante y detalle de productos, cantidades,
  unidades y vencimientos.
- **FR-011**: El comprobante MUST ser solo de recepción, no fiscal ni cobrable; una vez
  emitido MUST NOT poder editarse ni anularse.
- **FR-012**: El registro de donación, el incremento de stock y la generación del
  comprobante MUST completarse como una única operación: ante cualquier fallo no se
  conserva ningún cambio parcial.
- **FR-013**: El sistema MUST permitir consultar y filtrar donantes por nombre,
  donaciones por donante y fecha, productos por sus datos y stock por producto y
  vencimiento.
- **FR-014**: El sistema MUST permitir a depósito consultar y filtrar comprobantes por
  donante y fecha, manteniendo su asociación con la donación y su detalle.
- **FR-015**: Solo el rol de depósito MUST poder crear, modificar o dar de baja
  donantes, administrar productos, registrar y confirmar donaciones y modificar stock
  mediante recepciones.
- **FR-016**: Solo el rol de depósito MUST poder consultar y filtrar comprobantes de
  recepción.
- **FR-017**: Las nuevas operaciones MUST NOT alterar el comportamiento existente de
  remitos, confirmación de entregas ni control de vencimientos.
- **FR-018**: Los listados MUST presentar resultados paginados y respetar los filtros
  solicitados.

### Key Entities

- **Donante**: Organización o persona que entrega mercadería; incluye nombre, datos de
  contacto opcionales y estado activo/inactivo.
- **Producto**: Artículo identificable del inventario; incluye nombre, tipo de alimento
  y unidad de medida, y puede elegirse del catálogo o registrarse manualmente.
- **Donación**: Recepción confirmada de mercadería de un donante en una fecha; contiene
  una o más líneas y origina incrementos de stock y un comprobante.
- **Línea de donación**: Producto recibido, cantidad, unidad de medida, tipo de alimento
  y fecha de vencimiento; permite repetir un producto con vencimientos distintos.
- **Existencia**: Cantidad disponible de un producto en un vencimiento determinado;
  puede ser utilizada por las entregas existentes mientras cumpla sus reglas.
- **Comprobante de recepción**: Registro inmutable asociado de forma única a una
  donación; acredita el donante, la fecha y el detalle recibido, sin representar un
  cargo ni una obligación de pago.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: En todos los casos de prueba de una donación válida, el stock final por
  producto y vencimiento coincide exactamente con la suma de las cantidades recibidas.
- **SC-002**: El 100% de las donaciones confirmadas tiene exactamente un comprobante
  asociado con fecha, donante y todas las líneas recibidas.
- **SC-003**: En todos los casos de prueba con datos inválidos o fallos de operación,
  no queda ninguna donación parcial, incremento de stock ni comprobante asociado a la
  recepción rechazada.
- **SC-004**: En una muestra de al menos 20 búsquedas con filtros variados, el 100% de
  los resultados corresponde a los criterios solicitados, sin exponer comprobantes a
  roles no autorizados.
- **SC-005**: Al menos 9 de cada 10 personas de depósito de una prueba de uso pueden
  registrar una donación válida y localizar su comprobante en menos de 3 minutos.

## Assumptions

- Confirmar la donación es la acción que registra la recepción, incorpora el stock y
  emite el comprobante; no se requiere un ciclo de borradores para esta primera
  versión.
- El nombre es el único dato obligatorio del donante; los datos de contacto son
  opcionales porque no se especificaron campos adicionales.
- Dar de baja un donante lo desactiva para nuevas donaciones, conservando el historial
  asociado a las recepciones previas.
- El ingreso manual crea un producto identificable para el inventario si aún no existe;
  seleccionar un producto del catálogo no altera su ficha global.
- La unidad de medida y tipo de alimento quedan asociados a la línea recibida tal como
  se confirmó, aunque posteriormente se actualice la ficha del producto.
- La fecha de vencimiento igual a hoy se considera vencida, de acuerdo con las reglas
  de entrega y control de vencimientos ya existentes.
- Los comprobantes se conservan en el sistema y están disponibles para consulta por el
  personal de depósito; no se envían por correo ni tienen valor fiscal.
- Los listados usan paginación y filtros por los datos pertinentes de cada entidad.
- La autenticación y el rol de depósito reutilizan los mecanismos y usuarios existentes.
- Los umbrales de desempeño de las búsquedas se verificarán con una muestra de hasta
  10.000 registros; la prueba de uso de SC-005 se realizará con personas que conocen
  las tareas habituales del depósito.
