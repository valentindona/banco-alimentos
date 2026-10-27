CREATE TABLE donantes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    nombre VARCHAR(150) NOT NULL,
    correo VARCHAR(254),
    telefono VARCHAR(50),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    PRIMARY KEY (id)
);

CREATE INDEX ix_donantes_nombre ON donantes (nombre);
CREATE INDEX ix_donantes_activo ON donantes (activo);

CREATE TABLE donantes_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    nombre VARCHAR(150),
    correo VARCHAR(254),
    telefono VARCHAR(50),
    activo BOOLEAN,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_donantes_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);

CREATE TABLE donaciones (
    id BIGINT NOT NULL AUTO_INCREMENT,
    donante_id BIGINT NOT NULL,
    fecha DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_donaciones_donante FOREIGN KEY (donante_id) REFERENCES donantes (id)
);

CREATE INDEX ix_donaciones_donante_fecha ON donaciones (donante_id, fecha);

CREATE TABLE donaciones_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    donante_id BIGINT,
    fecha DATE,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_donaciones_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);

CREATE TABLE items_donacion (
    id BIGINT NOT NULL AUTO_INCREMENT,
    donacion_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    producto_nombre VARCHAR(120) NOT NULL,
    tipo_alimento VARCHAR(80) NOT NULL,
    unidad_medida VARCHAR(50) NOT NULL,
    cantidad DECIMAL(12, 3) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_items_donacion_cantidad_positiva CHECK (cantidad > 0),
    CONSTRAINT fk_items_donacion_donacion FOREIGN KEY (donacion_id) REFERENCES donaciones (id),
    CONSTRAINT fk_items_donacion_producto FOREIGN KEY (producto_id) REFERENCES productos (id)
);

CREATE INDEX ix_items_donacion_donacion ON items_donacion (donacion_id);
CREATE INDEX ix_items_donacion_producto_vencimiento ON items_donacion (producto_id, fecha_vencimiento);

CREATE TABLE items_donacion_aud (
    id BIGINT NOT NULL,
    REV INTEGER NOT NULL,
    REVTYPE TINYINT,
    donacion_id BIGINT,
    producto_id BIGINT,
    producto_nombre VARCHAR(120),
    tipo_alimento VARCHAR(80),
    unidad_medida VARCHAR(50),
    cantidad DECIMAL(12, 3),
    fecha_vencimiento DATE,
    PRIMARY KEY (id, REV),
    CONSTRAINT fk_items_donacion_aud_rev FOREIGN KEY (REV) REFERENCES REVINFO (REV)
);

ALTER TABLE productos ADD tipo_alimento VARCHAR(80);
ALTER TABLE productos_aud ADD tipo_alimento VARCHAR(80);

INSERT INTO productos (nombre, descripcion, unidad_medida, activo)
SELECT 'Leche', NULL, 'litros', TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE LOWER(nombre) = LOWER('Leche'));
UPDATE productos
SET tipo_alimento = 'Lácteo',
    unidad_medida = 'litros'
WHERE LOWER(nombre) = LOWER('Leche');

INSERT INTO productos (nombre, descripcion, unidad_medida, activo)
SELECT 'Harina', NULL, 'kilos', TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE LOWER(nombre) = LOWER('Harina'));
UPDATE productos
SET tipo_alimento = 'Harina',
    unidad_medida = 'kilos'
WHERE LOWER(nombre) = LOWER('Harina');

INSERT INTO productos (nombre, descripcion, unidad_medida, activo)
SELECT 'Galletitas', NULL, 'paquetes', TRUE
WHERE NOT EXISTS (SELECT 1 FROM productos WHERE LOWER(nombre) = LOWER('Galletitas'));
UPDATE productos
SET tipo_alimento = 'Galletitas',
    unidad_medida = 'paquetes'
WHERE LOWER(nombre) = LOWER('Galletitas');

ALTER TABLE facturas ${facturasInstitucionNullable};
ALTER TABLE facturas ${facturasImporteNullable};
ALTER TABLE facturas ADD donante_id BIGINT;
ALTER TABLE facturas ADD donacion_id BIGINT;

ALTER TABLE facturas
    ADD CONSTRAINT fk_facturas_donante FOREIGN KEY (donante_id) REFERENCES donantes (id);
ALTER TABLE facturas
    ADD CONSTRAINT fk_facturas_donacion FOREIGN KEY (donacion_id) REFERENCES donaciones (id);

CREATE INDEX ix_facturas_donante_fecha ON facturas (donante_id, fecha);
CREATE UNIQUE INDEX uq_facturas_donacion_id ON facturas (donacion_id);

ALTER TABLE facturas_aud ADD donante_id BIGINT;
ALTER TABLE facturas_aud ADD donacion_id BIGINT;
