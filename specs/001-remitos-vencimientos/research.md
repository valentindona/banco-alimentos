# Research: Entrega de mercadería con remitos y control de vencimientos

## Decisiones

### Confirmación atómica del remito

- **Decision**: Confirmar en una transacción de service que bloquea el remito y todas
  las existencias solicitadas con bloqueo de escritura pesimista. Adquirir los bloqueos
  en orden estable por identificador, validar el estado, fechas y cantidades de todas
  las líneas, descontar todas las cantidades y cambiar el remito a confirmado antes de
  completar la transacción.
- **Rationale**: Los borradores no reservan stock; el bloqueo serializa confirmaciones
  concurrentes sobre las mismas existencias. Una excepción antes del commit revierte
  cualquier modificación, evitando descuentos parciales. Bloquear el remito impide que
  una doble confirmación descuente dos veces.
- **Alternatives considered**: Actualizaciones SQL condicionales (`cantidad >= cantidad
  solicitada`) dentro de la misma transacción; control optimista con versión y política
  de reintento. El bloqueo pesimista permite validar el conjunto de líneas antes de
  modificarlo con el patrón JPA definido por el stack.
- **Sources**: [Spring Data JPA transactionality](https://docs.spring.io/spring-data/jpa/reference/jpa/transactions.html),
  [Spring Data JPA locking](https://docs.spring.io/spring-data/jpa/reference/jpa/locking.html),
  [MySQL InnoDB locking reads](https://dev.mysql.com/doc/refman/8.4/en/innodb-locking-reads.html).
  El acceso a la documentación MySQL no pudo verificarse desde la investigación; probar
  el comportamiento con MySQL real durante implementación.

### Identidad y consistencia de las existencias

- **Decision**: Identificar el stock por producto y fecha de vencimiento; mantener una
  restricción única para esa combinación y una restricción de cantidad no negativa.
  Identificar una línea por remito y existencia para impedir duplicados.
- **Rationale**: Corresponde al modelo explícito de stock por vencimiento y añade
  protección en persistencia además de la validación del service.
- **Alternatives considered**: Permitir líneas repetidas y agregarlas al confirmar, o
  representar cada recepción como un lote independiente. No se seleccionan porque la
  especificación define la existencia por producto y vencimiento y prohíbe repetir esa
  combinación en el remito.
- **Assumption**: Una fecha de vencimiento ausente puede existir para reflejar datos
  incompletos, pero nunca será elegible para entrega.

### Límite de vencimiento

- **Decision**: Considerar no entregable la existencia cuando
  `fechaVencimiento <= fechaNegocio`, tanto al agregarla como al confirmar un remito.
- **Rationale**: La especificación declara que el vencimiento de hoy ya es vencido.
  Esta regla explícita prevalece sobre convenciones alternativas que consideran válida
  toda la fecha impresa.
- **Alternatives considered**: Tratar como vencido solo cuando la fecha sea anterior a
  hoy. Se descarta por contradecir FR-005 y FR-011.

### Tarea diaria y avisos persistidos

- **Decision**: La tarea diaria llama a un service transaccional que recupera todas las
  existencias vencidas aún no marcadas, actualiza su estado y crea avisos; también
  encuentra las existencias vigentes que vencen entre mañana y tres días inclusive.
  Consultas con claves naturales únicas hacen el proceso repetible sin duplicados.
- **Rationale**: Consultar todas las filas atrasadas permite recuperarse de una
  interrupción del servicio. La idempotencia en base de datos protege contra
  reinicios y ejecuciones repetidas mejor que depender solo del scheduler.
- **Alternatives considered**: Emitir avisos solo para la fecha del día o usar una
  dependencia de bloqueo distribuido. La primera alternativa pierde avisos tras una
  interrupción; la segunda no es necesaria para la instancia única asumida y no
  sustituye la idempotencia persistente.
- **Assumption**: La tarea es diaria y de instancia única en esta entrega. Si el servicio
  se escala a varias réplicas, se debe agregar coordinación distribuida sin cambiar las
  reglas idempotentes.
- **Sources**: [Spring scheduling](https://docs.spring.io/spring-framework/reference/integration/scheduling.html),
  [ShedLock behavior and caveats](https://github.com/lukas-krecan/ShedLock).

### JWT, roles y provisionamiento de usuarios

- **Decision**: Usar una cadena de filtros sin sesión para emitir/verificar JWT con jjwt.
  Validar firma, algoritmo permitido y vencimiento; incluir solo el identificador del
  usuario y los roles `user_deposito`, `user_institucional` y `user_tesoreria`. Proteger
  cada ruta por rol y no confiar en ocultamiento de acciones en pantalla.
- **Rationale**: La aplicación requiere autenticación local sencilla y autorización
  explícita por endpoint. JWT firmado no debe tratarse como contenido confidencial.
- **Alternatives considered**: Delegar identidad a un proveedor externo con JWKS y
  Spring Resource Server; no hay proveedor externo definido para este proyecto nuevo.
- **Decision**: Provisionar cuentas mediante un proceso protegido, almacenar solo
  contraseñas con hash adaptativo y exigir una clave de firma configurada fuera del
  repositorio. El arranque de producción debe fallar si falta una clave válida.
- **Alternatives considered**: Registro público o usuarios/passwords predefinidos en
  código/fixtures; se descartan por no ser necesarios y por el riesgo de credenciales
  conocidas.
- **Sources**: [JJWT](https://github.com/jwtk/jjwt),
  [Spring Security password storage](https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/index.html),
  [JWT Best Current Practices, RFC 8725](https://www.rfc-editor.org/rfc/rfc8725),
  [Spring Boot externalized configuration](https://docs.spring.io/spring-boot/3.4/reference/features/external-config.html).

### Ámbito de Hibernate Envers

- **Decision**: Auditar Producto, Stock, Institucion, Remito, DetalleRemito y Factura;
  no auditar hashes de contraseñas ni registros operativos de Notificacion.
- **Rationale**: Mantiene historial para cambios de inventario y entregas sin registrar
  secretos o generar ruido de auditoría por eventos operativos. Envers no sustituye
  registros de seguridad.
- **Alternatives considered**: Auditar cada fila incluyendo usuarios y avisos; se
  descarta por exposición de información sensible y crecimiento innecesario.
- **Sources**: [Hibernate Envers user guide](https://docs.hibernate.org/orm/6.6/userguide/html_single/#envers),
  [Hibernate `@Audited` API](https://docs.hibernate.org/orm/6.6/javadocs/org/hibernate/envers/Audited.html).

### Consulta de facturas de solo lectura

- **Decision**: Incluir un módulo Factura mínimo, con listados/filtrado de solo lectura
  para depósito y sin operaciones de creación, cobro ni modificación.
- **Rationale**: Resuelve el requisito de consulta de la spec sin alterar la generación
  de facturas ni desarrollar la feature financiera completa.
- **Alternative considered**: Diferir la consulta hasta la spec de facturación; se
  descarta siguiendo la elección del usuario de incluir el módulo mínimo de consulta.
- **Assumption**: La proyección mínima usa número, institución, fecha, estado e importe.
  El esquema deberá adaptarse si una futura especificación de facturación establece
  atributos distintos.

### Esquema y migraciones

- **Decision**: Usar Flyway para aplicar migraciones SQL versionadas; validar el esquema
  MySQL al iniciar la aplicación y ejecutar los mismos cambios en H2 compatible para
  pruebas.
- **Rationale**: No existe esquema previo y la aplicación persiste datos de entregas e
  inventario. Versionar el esquema permite que los entornos de equipo se creen y
  evolucionen reproduciblemente.
- **Alternatives considered**: `ddl-auto=update` o scripts de inicialización sin
  versionado. Se descartan para evitar cambios automáticos no revisados sobre datos
  persistentes.
