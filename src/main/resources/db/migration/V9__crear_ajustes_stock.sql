CREATE TABLE ajustes_stock (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    fecha_vencimiento DATE,
    usuario_id BIGINT NOT NULL,
    fecha_hora TIMESTAMP(6) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    cantidad_anterior DECIMAL(12, 3) NOT NULL,
    cantidad_nueva DECIMAL(12, 3) NOT NULL,
    motivo TEXT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_ajustes_stock_tipo CHECK (
        CASE WHEN tipo = 'CORRECCION' THEN TRUE ELSE tipo = 'BAJA' END
    ),
    CONSTRAINT chk_ajustes_stock_cantidades CHECK (cantidad_anterior >= 0 AND cantidad_nueva >= 0),
    CONSTRAINT chk_ajustes_stock_baja CHECK (tipo <> 'BAJA' OR cantidad_nueva < cantidad_anterior),
    CONSTRAINT fk_ajustes_stock_stock FOREIGN KEY (stock_id) REFERENCES stock (id),
    CONSTRAINT fk_ajustes_stock_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT fk_ajustes_stock_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id)
);

CREATE INDEX ix_ajustes_stock_producto ON ajustes_stock (producto_id);
CREATE INDEX ix_ajustes_stock_vencimiento ON ajustes_stock (fecha_vencimiento);
CREATE INDEX ix_ajustes_stock_fecha_hora ON ajustes_stock (fecha_hora);
CREATE INDEX ix_ajustes_stock_usuario ON ajustes_stock (usuario_id);
CREATE INDEX ix_ajustes_stock_tipo ON ajustes_stock (tipo);

CREATE TABLE ajustes_stock_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    stock_id BIGINT,
    producto_id BIGINT,
    fecha_vencimiento DATE,
    usuario_id BIGINT,
    fecha_hora TIMESTAMP(6),
    tipo VARCHAR(20),
    cantidad_anterior DECIMAL(12, 3),
    cantidad_nueva DECIMAL(12, 3),
    motivo TEXT,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_ajustes_stock_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);
