CREATE TABLE remitos (
    id BIGINT NOT NULL AUTO_INCREMENT,
    institucion_id BIGINT NOT NULL,
    institucion_nombre VARCHAR(150) NOT NULL,
    institucion_domicilio VARCHAR(250) NOT NULL,
    fecha_entrega DATE NOT NULL,
    estado VARCHAR(20) NOT NULL,
    fecha_creacion TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    fecha_confirmacion TIMESTAMP(6),
    creado_por_usuario_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_remito_fecha_confirmacion CHECK (
        (estado = 'BORRADOR' AND fecha_confirmacion IS NULL)
        OR (estado = 'CONFIRMADO' AND fecha_confirmacion IS NOT NULL)
    ),
    CONSTRAINT fk_remitos_institucion FOREIGN KEY (institucion_id) REFERENCES instituciones (id),
    CONSTRAINT fk_remitos_creado_por FOREIGN KEY (creado_por_usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX ix_remitos_institucion_fecha ON remitos (institucion_id, fecha_entrega);
CREATE INDEX ix_remitos_estado ON remitos (estado);

CREATE TABLE detalles_remito (
    id BIGINT NOT NULL AUTO_INCREMENT,
    remito_id BIGINT NOT NULL,
    stock_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    producto_nombre VARCHAR(120) NOT NULL,
    cantidad DECIMAL(12, 3) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_detalle_remito_stock UNIQUE (remito_id, stock_id),
    CONSTRAINT chk_detalle_remito_cantidad_positiva CHECK (cantidad > 0),
    CONSTRAINT fk_detalle_remito_remito FOREIGN KEY (remito_id) REFERENCES remitos (id),
    CONSTRAINT fk_detalle_remito_stock FOREIGN KEY (stock_id) REFERENCES stock (id),
    CONSTRAINT fk_detalle_remito_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
);

CREATE TABLE remitos_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    institucion_id BIGINT,
    institucion_nombre VARCHAR(150),
    institucion_domicilio VARCHAR(250),
    fecha_entrega DATE,
    estado VARCHAR(20),
    fecha_creacion TIMESTAMP(6),
    fecha_confirmacion TIMESTAMP(6),
    creado_por_usuario_id BIGINT,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_remitos_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);

CREATE TABLE detalles_remito_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    remito_id BIGINT,
    stock_id BIGINT,
    producto_id BIGINT,
    fecha_vencimiento DATE,
    producto_nombre VARCHAR(120),
    cantidad DECIMAL(12, 3),
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_detalles_remito_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);
