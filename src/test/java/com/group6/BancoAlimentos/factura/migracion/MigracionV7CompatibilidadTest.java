package com.group6.BancoAlimentos.factura.migracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.DriverManager;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataAccessException;

class MigracionV7CompatibilidadTest {

    @Test
    void migraV1aV7ConservandoFacturasLegadasYAdmitiendoComprobantesDeDonacion() throws Exception {
        String databaseName = "migracion_v7_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .placeholders(Map.of(
                            "facturasInstitucionNullable", "ALTER COLUMN institucion_id SET NULL",
                            "facturasImporteNullable", "ALTER COLUMN importe SET NULL"))
                    .target(MigrationVersion.fromVersion("6"))
                    .load()
                    .migrate();

            jdbc.update("""
                    INSERT INTO instituciones (id, nombre, domicilio, estado_pago, activa)
                    VALUES (910001, 'Institución legado', 'Dirección', 'AL_DIA', TRUE)
                    """);
            jdbc.update("""
                    INSERT INTO facturas (id, numero, institucion_id, fecha, estado, importe)
                    VALUES (910001, 'F-LEGADA-910001', 910001, DATE '2024-03-15', 'PENDIENTE', 1250.75)
                    """);

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .placeholders(Map.of(
                            "facturasInstitucionNullable", "ALTER COLUMN institucion_id SET NULL",
                            "facturasImporteNullable", "ALTER COLUMN importe SET NULL"))
                    .load()
                    .migrate();

            assertEquals(
                    "F-LEGADA-910001",
                    jdbc.queryForObject("SELECT numero FROM facturas WHERE id = 910001", String.class));
            assertEquals(
                    910001L,
                    jdbc.queryForObject("SELECT institucion_id FROM facturas WHERE id = 910001", Long.class));
            assertEquals(
                    Date.valueOf(LocalDate.of(2024, 3, 15)),
                    jdbc.queryForObject("SELECT fecha FROM facturas WHERE id = 910001", Date.class));
            assertEquals(
                    "PENDIENTE",
                    jdbc.queryForObject("SELECT estado FROM facturas WHERE id = 910001", String.class));
            assertEquals(
                    new BigDecimal("1250.75"),
                    jdbc.queryForObject("SELECT importe FROM facturas WHERE id = 910001", BigDecimal.class));

            jdbc.update("INSERT INTO donantes (id, nombre, activo) VALUES (910001, 'Donante prueba', TRUE)");
            jdbc.update("INSERT INTO donaciones (id, donante_id, fecha) VALUES (910001, 910001, DATE '2026-10-26')");
            jdbc.update("""
                    INSERT INTO facturas (numero, institucion_id, donante_id, donacion_id, fecha, estado, importe)
                    VALUES ('910002', NULL, 910001, 910001, DATE '2026-10-26', 'EMITIDA', NULL)
                    """);

            assertEquals(
                    1,
                    jdbc.queryForObject("""
                            SELECT COUNT(*) FROM facturas
                            WHERE institucion_id IS NULL AND donante_id = 910001 AND donacion_id = 910001
                            """, Integer.class));
            assertThrows(DataAccessException.class, () -> jdbc.update("""
                    INSERT INTO facturas (numero, institucion_id, donante_id, donacion_id, fecha, estado, importe)
                    VALUES ('duplicada', NULL, 910001, 910001, DATE '2026-10-26', 'EMITIDA', NULL)
                    """));

            assertTrue(tieneColumna(jdbc, "FACTURAS_AUD", "DONANTE_ID"));
            assertTrue(tieneColumna(jdbc, "FACTURAS_AUD", "DONACION_ID"));
            assertTrue(tieneColumna(jdbc, "PRODUCTOS_AUD", "TIPO_ALIMENTO"));
            assertTrue(tieneTabla(jdbc, "DONANTES_AUD"));
            assertTrue(tieneTabla(jdbc, "DONACIONES_AUD"));
            assertTrue(tieneTabla(jdbc, "ITEMS_DONACION_AUD"));
            assertEquals("Lácteo", valorProducto(jdbc, "Leche", "tipo_alimento"));
            assertEquals("litros", valorProducto(jdbc, "Leche", "unidad_medida"));
            assertEquals("Harina", valorProducto(jdbc, "Harina", "tipo_alimento"));
            assertEquals("kilos", valorProducto(jdbc, "Harina", "unidad_medida"));
            assertEquals("Galletitas", valorProducto(jdbc, "Galletitas", "tipo_alimento"));
            assertEquals("paquetes", valorProducto(jdbc, "Galletitas", "unidad_medida"));
        } finally {
            try (var connection = DriverManager.getConnection(url, "sa", "");
                    var statement = connection.createStatement()) {
                statement.execute("SHUTDOWN");
            }
        }
    }

    private boolean tieneColumna(JdbcTemplate jdbc, String tabla, String columna) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT COUNT(*) > 0
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Boolean.class, tabla, columna));
    }

    private boolean tieneTabla(JdbcTemplate jdbc, String tabla) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT COUNT(*) > 0
                FROM INFORMATION_SCHEMA.TABLES
                WHERE TABLE_NAME = ?
                """, Boolean.class, tabla));
    }

    private String valorProducto(JdbcTemplate jdbc, String nombre, String campo) {
        return jdbc.queryForObject(
                "SELECT " + campo + " FROM productos WHERE LOWER(nombre) = LOWER(?)",
                String.class,
                nombre);
    }
}
