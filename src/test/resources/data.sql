INSERT INTO instituciones (id, nombre, domicilio, estado_pago, activa)
VALUES (900001, 'Institución de prueba', 'Calle de prueba 123', 'AL_DIA', TRUE);

INSERT INTO productos (id, nombre, descripcion, unidad_medida, activo)
VALUES (900001, 'Producto de prueba', NULL, 'unidad', TRUE);

INSERT INTO stock (id, producto_id, fecha_vencimiento, cantidad_disponible, estado)
VALUES (900001, 900001, DATE '2030-12-31', 25.000, 'DISPONIBLE');

INSERT INTO usuarios (id, nombre_usuario, clave_hash, rol, institucion_id, activo)
VALUES (900001, 'usuario-fixture', '$2a$10$0wicwWIt1O247XHV1erqrOCBd8IG1UJlark5oczqxWAeVw/59pzWG',
        'USER_DEPOSITO', NULL, TRUE);
