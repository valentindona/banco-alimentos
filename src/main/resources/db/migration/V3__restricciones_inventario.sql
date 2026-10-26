ALTER TABLE stock
    ADD CONSTRAINT chk_stock_cantidad_no_negativa CHECK (cantidad_disponible >= 0);

CREATE UNIQUE INDEX uq_stock_producto_vencimiento
    ON stock (producto_id, fecha_vencimiento);

CREATE INDEX ix_stock_vencimiento_estado
    ON stock (fecha_vencimiento, estado);
