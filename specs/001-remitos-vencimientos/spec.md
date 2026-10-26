# Feature Specification: Entrega de mercadería con remitos y control de vencimientos

**Feature Branch**: `001-remitos-vencimientos`

**Created**: 2026-10-26

**Status**: Draft

**Input**: User description: "Permitir al personal del depósito registrar entregas de mercadería a instituciones mediante remitos, seleccionando manualmente el vencimiento del stock y evitando toda entrega de productos vencidos. Avisar en pantalla al depósito cuando haya stock próximo a vencer (3 días o menos) o vencido. Al confirmar, descontar exactamente las cantidades del vencimiento elegido. Permitir consultar y filtrar remitos, productos, stock e instituciones según permisos; depósito también consulta facturas y tesorería consulta instituciones. Una institución en deuda puede recibir entregas. No cambiar donaciones ni facturación."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Registrar y confirmar una entrega segura (Priority: P1)

Una persona de depósito prepara un remito para una institución existente, elige
explícitamente el vencimiento de cada producto y la cantidad que se entregará, revisa
los datos y confirma la entrega. El sistema rechaza artículos vencidos o sin vencimiento
registrado, evita entregas superiores al stock disponible y descuenta el stock solo al
confirmar correctamente el remito.

**Why this priority**: Registrar entregas correctas sin distribuir alimentos vencidos
es el objetivo principal del flujo del depósito.

**Independent Test**: Preparar y confirmar un remito con stock disponible y vigente;
verificar que el remito refleja la institución, fecha, productos y vencimientos elegidos,
y que el stock de cada combinación disminuye exactamente según la cantidad entregada.
Repetir con un producto vencido y con stock insuficiente; ambos casos deben rechazarse
sin cambiar el remito confirmado ni descontar stock.

**Acceptance Scenarios**:

1. **Given** una institución existente y stock vigente disponible en varios
   vencimientos, **When** depósito prepara un remito y elige una fecha de vencimiento
   específica, **Then** el remito registra esa elección y el sistema no sustituye el
   vencimiento por otro.
2. **Given** un artículo cuyo vencimiento es hoy o anterior, **When** depósito intenta
   agregarlo al remito, **Then** el sistema lo rechaza con un mensaje claro y el remito
   no se modifica.
3. **Given** un remito preparado con productos vigentes, **When** uno de ellos vence
   antes de la confirmación, **Then** la confirmación se rechaza y no se descuenta stock.
4. **Given** un remito cuya cantidad solicitada supera el stock vigente de uno de los
   vencimientos elegidos, **When** depósito confirma el remito, **Then** se rechaza la
   operación completa y no se descuenta stock de ningún artículo.
5. **Given** un remito válido, **When** depósito lo confirma, **Then** el stock de cada
   producto y vencimiento elegido disminuye exactamente en la cantidad indicada.
6. **Given** una institución con estado de pago "en deuda", **When** depósito prepara y
   confirma una entrega válida, **Then** el sistema permite la entrega.
7. **Given** un remito sin artículos o con el mismo producto y vencimiento agregado más
   de una vez, **When** depósito intenta confirmarlo o duplicar el artículo,
   **Then** el sistema lo rechaza y conserva el stock.
8. **Given** una institución inexistente o sin los datos obligatorios para una entrega,
   **When** depósito intenta preparar un remito, **Then** el sistema informa el motivo
   y no crea una entrega.

---

### User Story 2 - Detectar stock próximo a vencer o vencido (Priority: P2)

Al ingresar al sistema, el personal de depósito ve avisos de stock disponible que vence
pronto o que ya venció. El sistema trata el stock vencido como no entregable sin requerir
que una persona lo bloquee manualmente.

**Why this priority**: Los avisos anticipados ayudan a organizar el depósito y el bloqueo
por vencimiento evita que un producto inseguro llegue a una institución.

**Independent Test**: Preparar existencias con fechas de vencimiento a 3 días, a 4 días,
hoy y en el pasado. Al ingresar como depósito, verificar que se avisa sobre el producto
que vence en 3 días y el stock vencido, no sobre el de 4 días; confirmar además que el
stock vencido no puede incluirse en un remito.

**Acceptance Scenarios**:

1. **Given** stock disponible que vence dentro de 3 días, **When** una persona de depósito
   ingresa al sistema, **Then** ve un aviso que identifica el producto, su vencimiento y
   la cantidad disponible.
2. **Given** stock disponible que vence dentro de 4 días, **When** depósito consulta los
   avisos, **Then** ese stock no aparece como próximo a vencer.
3. **Given** stock que alcanza su fecha de vencimiento o ya la pasó, **When** depósito
   ingresa al sistema, **Then** el stock aparece bloqueado y se muestra un aviso en
   pantalla; no puede seleccionarse para una entrega.
4. **Given** stock de un producto sin fecha de vencimiento registrada, **When** depósito
   intenta usarlo en un remito, **Then** el sistema lo considera no elegible y comunica
   que hace falta registrar una fecha válida.
5. **Given** avisos de vencimiento o stock vencido, **When** una persona que no pertenece
   a depósito consulta el sistema, **Then** no ve esos avisos.

---

### User Story 3 - Consultar y filtrar información relacionada (Priority: P3)

Los usuarios autorizados consultan remitos, productos, stock e instituciones con filtros
pertinentes a cada información. El personal de depósito también consulta instituciones
y facturas; tesorería consulta instituciones. Las personas usuarias institucionales
consultan información relacionada con su propia institución, sin registrar remitos.

**Why this priority**: La consulta facilita el seguimiento de las entregas sin ampliar
las facultades de registro del personal que no pertenece al depósito.

**Independent Test**: Ingresar con cada rol, aplicar filtros a la información que le
corresponde y comprobar que obtiene resultados coincidentes; intentar registrar un
remito con cada rol no autorizado y verificar que se deniega el acceso.

**Acceptance Scenarios**:

1. **Given** remitos, productos y existencias con instituciones, fechas, vencimientos y
   estados distintos, **When** depósito filtra cada listado por los datos propios de
   esa información, **Then** solo aparecen coincidencias y los resultados pueden
   consultarse por páginas.
2. **Given** instituciones y facturas registradas, **When** depósito consulta y filtra
   esos listados, **Then** ve únicamente los resultados que coinciden con sus filtros.
3. **Given** instituciones registradas, **When** tesorería consulta y filtra instituciones,
   **Then** ve las instituciones coincidentes.
4. **Given** remitos asociados a distintas instituciones, **When** una persona
   institucional consulta sus entregas, **Then** solo ve información de su propia
   institución.
5. **Given** un usuario de tesorería o institucional, **When** intenta registrar un
   remito, **Then** el sistema deniega la operación y no crea ni modifica remitos.

### Edge Cases

- El vencimiento es hoy: se trata como vencido y no puede agregarse ni confirmarse.
- Un artículo vence después de prepararse el remito, pero antes de confirmarlo: se
  vuelve a validar al confirmar y se rechaza la entrega sin descontar stock.
- La cantidad solicitada supera el stock del vencimiento seleccionado: se rechaza el
  remito completo, sin descuentos parciales.
- El remito no contiene artículos o repite el mismo producto y vencimiento: no se puede
  confirmar; el mismo producto con vencimientos distintos sí puede aparecer en líneas
  separadas.
- El producto vence exactamente dentro de 3 días: debe generar un aviso.
- No hay fecha de vencimiento registrada: el stock no se puede entregar hasta que se
  registre una fecha válida.
- La institución no existe o carece de datos obligatorios: no se crea el remito y se
  informa el motivo.
- Un usuario sin rol de depósito intenta registrar un remito: se deniega el acceso.
- Dos remitos intentan consumir simultáneamente el mismo stock: al confirmar cada uno se
  vuelve a verificar la disponibilidad y no se permite un descuento superior al stock.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: El sistema MUST mantener las cantidades disponibles separadas por producto
  y fecha de vencimiento, de modo que un producto pueda tener existencias en varias
  fechas.
- **FR-002**: El sistema MUST permitir que depósito prepare un remito con una institución
  existente y completa, fecha de entrega y una o más líneas de mercadería con cantidad y
  vencimiento seleccionado.
- **FR-003**: El personal de depósito MUST seleccionar explícitamente el vencimiento de
  cada línea; el sistema MUST NOT escoger o sustituirlo automáticamente.
- **FR-004**: Solo el rol de depósito MUST poder crear, modificar o confirmar remitos.
  Los usuarios de tesorería e institucionales MUST NOT poder registrar ni modificar
  remitos.
- **FR-005**: El sistema MUST rechazar al agregar y al confirmar un producto cuyo
  vencimiento sea hoy o anterior, sin alterar el remito ni descontar stock.
- **FR-006**: El sistema MUST volver a comprobar vencimientos y stock al confirmar un
  remito, para cubrir cambios ocurridos desde su preparación.
- **FR-007**: El sistema MUST rechazar la confirmación si cualquier línea solicita más
  cantidad que la disponible para el producto y vencimiento elegidos; el rechazo MUST
  dejar intactas todas las cantidades de stock del remito.
- **FR-008**: Al confirmar un remito válido, el sistema MUST descontar exactamente la
  cantidad de cada producto y vencimiento indicado.
- **FR-009**: El sistema MUST impedir la confirmación de remitos vacíos y el agregado
  duplicado del mismo producto con el mismo vencimiento. El mismo producto en
  vencimientos diferentes MUST admitirse como líneas separadas.
- **FR-010**: El sistema MUST mostrar a depósito, al ingresar, avisos por cada existencia
  con cantidad disponible cuyo vencimiento ocurra dentro de los próximos 3 días,
  incluyendo el límite de 3 días y excluyendo las existencias ya vencidas de esta
  categoría.
- **FR-011**: Desde la fecha de vencimiento inclusive, el sistema MUST tratar la
  existencia como vencida, bloquear su selección para entregas y mostrar a depósito un
  aviso de stock vencido.
- **FR-012**: El sistema MUST permitir filtrar remitos por institución, fecha y estado;
  productos por sus propios datos; y stock por producto, vencimiento y estado. Los
  listados principales MUST ofrecer navegación paginada.
- **FR-013**: El personal de depósito MUST poder consultar y filtrar instituciones y
  facturas. Tesorería MUST poder consultar y filtrar instituciones.
- **FR-014**: Los usuarios institucionales MUST poder consultar información de remitos
  relacionada exclusivamente con su institución asociada y MUST NOT ver datos de otras
  instituciones mediante esta consulta.
- **FR-015**: El sistema MUST permitir que una institución en estado de pago "en deuda"
  reciba entregas válidas; el estado de deuda MUST NOT bloquear ni alterar la
  confirmación.
- **FR-016**: Los avisos de vencimiento y de stock vencido MUST mostrarse únicamente a
  usuarios de depósito; el sistema MUST NOT enviar avisos por correo, SMS ni a otros
  roles.
- **FR-017**: Si la institución no existe o no tiene los datos obligatorios que requiere
  el registro de entregas, el sistema MUST rechazar el remito con un mensaje claro y sin
  crear una entrega.
- **FR-018**: El alcance MUST NOT cambiar el registro de donaciones ni la generación o
  lógica de facturas existente.

### Key Entities *(include if feature involves data)*

- **Producto**: Artículo del catálogo que puede estar disponible en más de un
  vencimiento.
- **Existencia por vencimiento**: Cantidad disponible de un producto para una fecha de
  vencimiento determinada; su estado de elegibilidad depende de la fecha y de la
  cantidad existente.
- **Remito**: Registro de una entrega a una institución, con fecha, estado y una o más
  líneas de mercadería.
- **Línea de remito**: Producto, vencimiento seleccionado y cantidad que se entrega.
- **Institución**: Destino de la entrega, con sus datos requeridos y estado de pago.
- **Aviso de vencimiento**: Información visible para depósito sobre existencias próximas
  a vencer o ya vencidas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: El 100% de los intentos de incluir o confirmar existencias vencidas,
  incluyendo las que vencen hoy, son rechazados y no cambian el stock.
- **SC-002**: El 100% de las existencias con cantidad disponible que vencen entre mañana
  y los próximos 3 días aparecen en los avisos de depósito; ninguna que vence en 4 días
  aparece como próxima a vencer.
- **SC-003**: En el 100% de las confirmaciones válidas, la diferencia de stock antes y
  después coincide exactamente con las cantidades del remito; una confirmación
  inválida produce cero descuentos parciales.
- **SC-004**: El 100% de los intentos de registrar remitos con roles de tesorería o
  institucional son denegados, mientras que las entregas válidas a instituciones en
  deuda son permitidas.
- **SC-005**: En una prueba de uso, al menos el 90% del personal de depósito completa un
  remito válido con vencimiento y cantidad correctos en menos de 3 minutos y puede
  identificar con claridad por qué se rechaza una operación inválida.

## Assumptions

- Los avisos próximos a vencer cubren existencias con fecha futura entre mañana y el
  tercer día inclusive; las existencias que vencen hoy pasan a la categoría de vencidas.
- La existencia se considera no elegible si carece de una fecha de vencimiento válida,
  hasta que se registre esa fecha.
- Los avisos se muestran al ingresar y describen cada producto y vencimiento con
  cantidad positiva; no se requieren confirmación de lectura ni avisos fuera de
  pantalla.
- Una línea se identifica para evitar duplicados por producto y vencimiento. El mismo
  producto puede incluirse en el remito para vencimientos distintos.
- Los datos obligatorios de una institución son los definidos por el registro actual de
  instituciones; este alcance no agrega ni redefine esos campos.
- Las personas institucionales tienen una institución asociada y solo consultan los
  remitos de esa institución. Tesorería consulta instituciones según los permisos
  existentes; los permisos para otros listados no se amplían.
- La disponibilidad se verifica nuevamente al confirmar. Preparar un remito no reserva
  stock y un rechazo por concurrencia no descuenta parcialmente ninguna línea.
- Los estados del remito incluyen, como mínimo, preparado y confirmado para distinguir
  las entregas pendientes de las realizadas.
- Donaciones, pagos y facturación existentes se mantienen fuera del alcance, salvo las
  consultas de facturas autorizadas a depósito.
