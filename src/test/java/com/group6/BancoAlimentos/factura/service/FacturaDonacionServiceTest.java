package com.group6.BancoAlimentos.factura.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@Import(FacturaService.class)
class FacturaDonacionServiceTest {

    @Autowired
    private FacturaService facturaService;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void emiteUnComprobantePorDonacionConNumeroIdYConservaFacturasLegadas() {
        Long donanteId = crearDonante();
        Long donacionId = crearDonacion(donanteId);
        LocalDate fecha = LocalDate.of(2026, 10, 26);

        var comprobante = facturaService.emitir(donanteId, donacionId, fecha);

        assertEquals(comprobante.getId().toString(), comprobante.getNumero());
        assertEquals("EMITIDA", comprobante.getEstado());
        assertNull(comprobante.getImporte());
        assertNull(comprobante.getInstitucionId());
        assertEquals(donanteId, comprobante.getDonanteId());
        assertEquals(donacionId, comprobante.getDonacionId());
        assertEquals(fecha, comprobante.getFecha());
        assertEquals(
                1,
                jdbc.queryForObject(
                        "SELECT COUNT(*) FROM facturas WHERE donacion_id = ? AND donante_id = ?",
                        Integer.class,
                        donacionId,
                        donanteId));

        assertThrows(
                ReglaNegocioException.class,
                () -> facturaService.emitir(donanteId, donacionId, fecha));

        assertEquals(
                "F-FIXTURE-900001",
                jdbc.queryForObject("SELECT numero FROM facturas WHERE id = 900001", String.class));
        assertEquals(
                900001L,
                jdbc.queryForObject("SELECT institucion_id FROM facturas WHERE id = 900001", Long.class));
        assertEquals(
                new BigDecimal("1250.00"),
                jdbc.queryForObject("SELECT importe FROM facturas WHERE id = 900001", BigDecimal.class));
        assertNotNull(comprobante.getId());
    }

    @Test
    void generaComprobanteFaltantePeroNoDuplicaElQueYaExiste() {
        Long donanteId = crearDonante();
        Long donacionId = crearDonacion(donanteId);

        var primero = facturaService.generarParaDonacion(donacionId);
        var segundo = facturaService.generarParaDonacion(donacionId);

        assertEquals(false, primero.existente());
        assertEquals(true, segundo.existente());
        assertEquals(primero.factura().getId(), segundo.factura().getId());
        assertEquals(
                1,
                jdbc.queryForObject("SELECT COUNT(*) FROM facturas WHERE donacion_id = ?", Integer.class, donacionId));
    }

    private Long crearDonante() {
        jdbc.update("INSERT INTO donantes (nombre, activo) VALUES ('Donante de prueba', TRUE)");
        return jdbc.queryForObject("SELECT MAX(id) FROM donantes", Long.class);
    }

    private Long crearDonacion(Long donanteId) {
        jdbc.update(
                "INSERT INTO donaciones (donante_id, fecha) VALUES (?, ?)",
                donanteId,
                LocalDate.of(2026, 10, 26));
        return jdbc.queryForObject("SELECT MAX(id) FROM donaciones", Long.class);
    }
}
