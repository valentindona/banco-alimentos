CREATE TABLE productos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(120) NOT NULL,
    descripcion VARCHAR(1000),
    unidad_medida VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE TABLE instituciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    domicilio VARCHAR(250) NOT NULL,
    estado_pago VARCHAR(20) NOT NULL,
    activa BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE TABLE stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    producto_id BIGINT NOT NULL,
    fecha_vencimiento DATE,
    cantidad_disponible DECIMAL(12, 3) NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_actualizacion TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_stock_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
);

CREATE TABLE usuarios (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre_usuario VARCHAR(100) NOT NULL,
    clave_hash VARCHAR(255) NOT NULL,
    rol VARCHAR(30) NOT NULL,
    institucion_id BIGINT,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    creado_en TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uq_usuarios_nombre_usuario UNIQUE (nombre_usuario),
    CONSTRAINT fk_usuarios_institucion FOREIGN KEY (institucion_id) REFERENCES instituciones (id)
);

CREATE TABLE productos_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    nombre VARCHAR(120),
    descripcion VARCHAR(1000),
    unidad_medida VARCHAR(50),
    activo BOOLEAN,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_productos_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);

CREATE TABLE instituciones_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    nombre VARCHAR(150),
    domicilio VARCHAR(250),
    estado_pago VARCHAR(20),
    activa BOOLEAN,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_instituciones_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);

CREATE TABLE stock_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    producto_id BIGINT,
    fecha_vencimiento DATE,
    cantidad_disponible DECIMAL(12, 3),
    estado VARCHAR(20),
    fecha_actualizacion TIMESTAMP(6),
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_stock_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);
