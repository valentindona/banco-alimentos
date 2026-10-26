package com.group6.BancoAlimentos.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VencimientoStockServiceTest {

    private VencimientoStockService servicio;
    private LocalDate fechaNegocio;

    @BeforeEach
    void prepararServicio() {
        servicio = new VencimientoStockService();
        fechaNegocio = LocalDate.of(2026, 10, 26);
    }

    @Test
    void marcaComoVencidoElStockQueVenceHoyOAntes() {
        Stock venceHoy = crearStock(fechaNegocio, EstadoStock.DISPONIBLE);
        Stock vencidoAnteriormente = crearStock(fechaNegocio.minusDays(1), EstadoStock.DISPONIBLE);

        servicio.actualizarEstado(venceHoy, fechaNegocio);
        servicio.actualizarEstado(vencidoAnteriormente, fechaNegocio);

        assertEquals(EstadoStock.VENCIDO, venceHoy.getEstado());
        assertEquals(EstadoStock.VENCIDO, vencidoAnteriormente.getEstado());
        assertFalse(servicio.esElegibleParaEntrega(venceHoy, fechaNegocio));
    }

    @Test
    void mantieneElegibleYMarcaComoProximoElStockVigenteDentroDeTresDias() {
        Stock venceEnTresDias = crearStock(fechaNegocio.plusDays(3), EstadoStock.DISPONIBLE);

        servicio.actualizarEstado(venceEnTresDias, fechaNegocio);

        assertEquals(EstadoStock.DISPONIBLE, venceEnTresDias.getEstado());
        assertTrue(servicio.esElegibleParaEntrega(venceEnTresDias, fechaNegocio));
        assertTrue(servicio.estaProximoAVencer(venceEnTresDias, fechaNegocio));
    }

    @Test
    void excluyeAvisosParaStockQueVenceEnCuatroDias() {
        Stock venceEnCuatroDias = crearStock(fechaNegocio.plusDays(4), EstadoStock.DISPONIBLE);

        assertTrue(servicio.esElegibleParaEntrega(venceEnCuatroDias, fechaNegocio));
        assertFalse(servicio.estaProximoAVencer(venceEnCuatroDias, fechaNegocio));
    }

    @Test
    void marcaComoSinVencimientoLasExistenciasSinFecha() {
        Stock sinFecha = crearStock(null, EstadoStock.DISPONIBLE);

        servicio.actualizarEstado(sinFecha, fechaNegocio);

        assertEquals(EstadoStock.SIN_VENCIMIENTO, sinFecha.getEstado());
        assertFalse(servicio.esElegibleParaEntrega(sinFecha, fechaNegocio));
        assertFalse(servicio.estaProximoAVencer(sinFecha, fechaNegocio));
    }

    @Test
    void noConsideraAvisableUnaExistenciaSinCantidadDisponible() {
        Stock sinCantidad = crearStock(fechaNegocio.plusDays(2), EstadoStock.DISPONIBLE);
        sinCantidad.setCantidadDisponible(BigDecimal.ZERO);

        assertFalse(servicio.estaProximoAVencer(sinCantidad, fechaNegocio));
    }

    private Stock crearStock(LocalDate fechaVencimiento, EstadoStock estado) {
        return Stock.builder()
                .id(12L)
                .productoId(7L)
                .fechaVencimiento(fechaVencimiento)
                .cantidadDisponible(new BigDecimal("5.000"))
                .estado(estado)
                .build();
    }
}
