package com.group6.BancoAlimentos.institucion.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DriverManager;
import java.util.Map;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class InstitucionMigracionTest {

    @Test
    void migraInstitucionesExistentesAgregandoTipoYColumnaDeAuditoria() throws Exception {
        String databaseName = "migracion_institucion_" + UUID.randomUUID().toString().replace("-", "");
        String url = "jdbc:h2:mem:" + databaseName + ";MODE=MySQL;DB_CLOSE_DELAY=-1";
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, "sa", "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        Map<String, String> placeholders = Map.of(
                "facturasInstitucionNullable", "ALTER COLUMN institucion_id SET NULL",
                "facturasImporteNullable", "ALTER COLUMN importe SET NULL");

        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .placeholders(placeholders)
                    .target(MigrationVersion.fromVersion("7"))
                    .load()
                    .migrate();

            jdbc.update("""
                    INSERT INTO instituciones (id, nombre, domicilio, estado_pago, activa)
                    VALUES (910001, 'Institución legado', 'Dirección', 'AL_DIA', TRUE)
                    """);

            Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .placeholders(placeholders)
                    .load()
                    .migrate();

            assertEquals(
                    "Sin especificar",
                    jdbc.queryForObject("SELECT tipo FROM instituciones WHERE id = 910001", String.class));
            assertTrue(tieneColumna(jdbc, "INSTITUCIONES_AUD", "TIPO"));
            assertEquals("YES", nullable(jdbc, "INSTITUCIONES", "TIPO"));
            assertEquals(80, longitud(jdbc, "INSTITUCIONES", "TIPO"));
            assertEquals(1, jdbc.update("""
                    INSERT INTO instituciones (nombre, domicilio, tipo, estado_pago, activa)
                    VALUES ('Sin tipo', 'Dirección', NULL, 'AL_DIA', TRUE)
                    """));
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

    private String nullable(JdbcTemplate jdbc, String tabla, String columna) {
        return jdbc.queryForObject("""
                SELECT IS_NULLABLE
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, String.class, tabla, columna);
    }

    private Integer longitud(JdbcTemplate jdbc, String tabla, String columna) {
        return jdbc.queryForObject("""
                SELECT CHARACTER_MAXIMUM_LENGTH
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, tabla, columna);
    }
}
