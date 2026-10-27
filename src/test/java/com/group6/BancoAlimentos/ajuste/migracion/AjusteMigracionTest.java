package com.group6.BancoAlimentos.ajuste.migracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class AjusteMigracionTest {

    @Test
    void creaTablasRestriccionesIndicesYRelacionesDeAjustes() throws Exception {
        String url = "jdbc:h2:mem:ajuste_" + UUID.randomUUID().toString().replace("-", "")
                + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource fuenteDatos = new DriverManagerDataSource(url, "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(fuenteDatos);

        try {
            Flyway.configure()
                    .dataSource(fuenteDatos)
                    .locations("classpath:db/migration")
                    .placeholders(Map.of(
                            "facturasInstitucionNullable", "ALTER COLUMN institucion_id SET NULL",
                            "facturasImporteNullable", "ALTER COLUMN importe SET NULL"))
                    .load()
                    .migrate();

            assertTrue(existeTabla(jdbc, "AJUSTES_STOCK"));
            assertTrue(existeTabla(jdbc, "AJUSTES_STOCK_AUD"));
            assertEquals(0, jdbc.queryForObject("""
                    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
                    WHERE CONSTRAINT_NAME = 'CHK_AJUSTES_STOCK_TIPO'
                    """, Integer.class));
            jdbc.update("INSERT INTO productos (id, nombre, unidad_medida) VALUES (900001, 'Migración', 'kg')");
            jdbc.update("""
                    INSERT INTO usuarios (id, nombre_usuario, clave_hash, rol)
                    VALUES (900001, 'migracion', 'hash', 'USER_DEPOSITO')
                    """);
            jdbc.update("""
                    INSERT INTO stock (id, producto_id, cantidad_disponible, estado)
                    VALUES (900001, 900001, 1.000, 'DISPONIBLE')
                    """);
            jdbc.update("""
                    INSERT INTO ajustes_stock (
                        id, stock_id, producto_id, usuario_id, fecha_hora, tipo,
                        cantidad_anterior, cantidad_nueva, motivo
                    ) VALUES (900001, 900001, 900001, 900001, CURRENT_TIMESTAMP, 'BAJA', 2, 1, 'Prueba')
                    """);
            jdbc.update("""
                    INSERT INTO ajustes_stock (
                        id, stock_id, producto_id, usuario_id, fecha_hora, tipo,
                        cantidad_anterior, cantidad_nueva, motivo
                    ) VALUES (900002, 900001, 900001, 900001, CURRENT_TIMESTAMP, 'CORRECCION', 1, 0, 'Corrección')
                    """);
            assertEquals(12, precision(jdbc, "AJUSTES_STOCK", "CANTIDAD_NUEVA"));
            assertEquals(3, escala(jdbc, "AJUSTES_STOCK", "CANTIDAD_NUEVA"));
            assertEquals("NO", nullable(jdbc, "AJUSTES_STOCK", "MOTIVO"));
            for (String columna : new String[] {
                "PRODUCTO_ID", "FECHA_VENCIMIENTO", "FECHA_HORA", "USUARIO_ID", "TIPO"
            }) {
                assertTrue(tieneIndice(jdbc, "AJUSTES_STOCK", columna), "Falta índice para " + columna);
            }
            assertTrue(tieneColumna(jdbc, "AJUSTES_STOCK_AUD", "REVTYPE"));
            assertTrue(tieneColumna(jdbc, "AJUSTES_STOCK_AUD", "MOTIVO"));
            assertTrue(tieneForeignKey(jdbc, "AJUSTES_STOCK", "STOCK_ID", "STOCK"));
            assertTrue(tieneForeignKey(jdbc, "AJUSTES_STOCK", "PRODUCTO_ID", "PRODUCTOS"));
            assertTrue(tieneForeignKey(jdbc, "AJUSTES_STOCK", "USUARIO_ID", "USUARIOS"));
            assertTrue(tieneForeignKey(jdbc, "AJUSTES_STOCK_AUD", "REV", "REVINFO"));
        } finally {
            try (var connection = DriverManager.getConnection(url, "sa", "");
                    var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    private boolean existeTabla(JdbcTemplate jdbc, String tabla) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT COUNT(*) > 0 FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?
                """, Boolean.class, tabla));
    }

    private boolean tieneColumna(JdbcTemplate jdbc, String tabla, String columna) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT COUNT(*) > 0 FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Boolean.class, tabla, columna));
    }

    private boolean tieneIndice(JdbcTemplate jdbc, String tabla, String columna) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT COUNT(*) > 0 FROM INFORMATION_SCHEMA.INDEX_COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Boolean.class, tabla, columna));
    }

    private boolean tieneForeignKey(JdbcTemplate jdbc, String tabla, String columna, String tablaReferenciada) {
        return Boolean.TRUE.equals(jdbc.execute((ConnectionCallback<Boolean>) connection -> {
            try (var claves = connection.getMetaData().getImportedKeys(connection.getCatalog(), null, tabla)) {
                while (claves.next()) {
                    if (columna.equalsIgnoreCase(claves.getString("FKCOLUMN_NAME"))
                            && tablaReferenciada.equalsIgnoreCase(claves.getString("PKTABLE_NAME"))) {
                        return true;
                    }
                }
                return false;
            }
        }));
    }

    private Integer precision(JdbcTemplate jdbc, String tabla, String columna) {
        return jdbc.queryForObject("""
                SELECT NUMERIC_PRECISION FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, tabla, columna);
    }

    private Integer escala(JdbcTemplate jdbc, String tabla, String columna) {
        return jdbc.queryForObject("""
                SELECT NUMERIC_SCALE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, tabla, columna);
    }

    private String nullable(JdbcTemplate jdbc, String tabla, String columna) {
        return jdbc.queryForObject("""
                SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, String.class, tabla, columna);
    }
}
