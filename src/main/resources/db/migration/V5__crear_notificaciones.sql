CREATE TABLE notificaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    stock_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    audiencia VARCHAR(30) NOT NULL,
    fecha_creacion TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    mensaje VARCHAR(500) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_notificaciones_stock_tipo UNIQUE (stock_id, tipo),
    CONSTRAINT fk_notificaciones_stock FOREIGN KEY (stock_id) REFERENCES stock (id)
);

CREATE INDEX ix_notificaciones_fecha_creacion ON notificaciones (fecha_creacion);
