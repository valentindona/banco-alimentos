CREATE TABLE facturas (
    id BIGINT NOT NULL AUTO_INCREMENT,
    numero VARCHAR(80) NOT NULL,
    institucion_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    estado VARCHAR(30) NOT NULL,
    importe DECIMAL(12, 2) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_facturas_institucion FOREIGN KEY (institucion_id) REFERENCES instituciones (id)
);

CREATE INDEX ix_facturas_institucion_fecha ON facturas (institucion_id, fecha);
CREATE INDEX ix_facturas_estado ON facturas (estado);

CREATE TABLE facturas_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    numero VARCHAR(80),
    institucion_id BIGINT,
    fecha DATE,
    estado VARCHAR(30),
    importe DECIMAL(12, 2),
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_facturas_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);
