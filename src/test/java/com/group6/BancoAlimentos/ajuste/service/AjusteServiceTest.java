package com.group6.BancoAlimentos.ajuste.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.ajuste.dto.AjusteSolicitud;
import com.group6.BancoAlimentos.ajuste.modelo.Ajuste;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import com.group6.BancoAlimentos.ajuste.repository.AjusteRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AjusteServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private AjusteRepository ajusteRepository;

    private AjusteService servicio;
    private Stock stock;
    private UsuarioAutenticado usuario;

    @BeforeEach
    void preparar() {
        servicio = new AjusteService(stockRepository, ajusteRepository);
        stock = Stock.builder()
                .id(25L)
                .productoId(7L)
                .fechaVencimiento(LocalDate.of(2026, 10, 1))
                .cantidadDisponible(new BigDecimal("10.000"))
                .estado(EstadoStock.VENCIDO)
                .build();
        usuario = new UsuarioAutenticado(9L, "deposito", List.of("user_deposito"), null);
        lenient().when(stockRepository.buscarTodosPorIdBloqueadosOrdenados(List.of(25L)))
                .thenReturn(List.of(stock));
        lenient().when(ajusteRepository.save(any(Ajuste.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));
    }

    @Test
    void registraBajaUsandoElStockBloqueadoYConservaSnapshotYEstado() {
        var respuesta = servicio.registrar(
                new AjusteSolicitud(25L, TipoAjuste.BAJA, new BigDecimal("2.500"), "  Mercadería dañada  "),
                usuario);

        assertEquals(new BigDecimal("7.500"), stock.getCantidadDisponible());
        assertEquals(EstadoStock.VENCIDO, stock.getEstado());
        assertEquals(7L, respuesta.productoId());
        assertEquals(LocalDate.of(2026, 10, 1), respuesta.fechaVencimiento());
        assertEquals(new BigDecimal("10.000"), respuesta.cantidadAnterior());
        assertEquals(new BigDecimal("7.500"), respuesta.cantidadNueva());
        assertEquals("Mercadería dañada", respuesta.motivo());
        verify(stockRepository).saveAndFlush(stock);
        verify(ajusteRepository).save(any(Ajuste.class));
    }

    @Test
    void permiteBajaCompletaYCorreccionAUnSaldoIgualAlActual() {
        var baja = servicio.registrar(
                new AjusteSolicitud(25L, TipoAjuste.BAJA, new BigDecimal("10.000"), "Baja total"),
                usuario);
        assertEquals(BigDecimal.ZERO.setScale(3), baja.cantidadNueva());
        stock.setCantidadDisponible(new BigDecimal("4.250"));
        when(ajusteRepository.save(any(Ajuste.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        var correccion = servicio.registrar(
                new AjusteSolicitud(25L, TipoAjuste.CORRECCION, new BigDecimal("4.250"), "Recuento"),
                usuario);

        assertEquals(new BigDecimal("4.250"), correccion.cantidadAnterior());
        assertEquals(new BigDecimal("4.250"), correccion.cantidadNueva());
    }

    @Test
    void rechazaBajaInvalidaSinEscribirYCorreccionNegativa() {
        for (BigDecimal cantidad : List.of(
                BigDecimal.ZERO, new BigDecimal("-1.000"), new BigDecimal("10.001"))) {
            assertThrows(ReglaNegocioException.class, () -> servicio.registrar(
                    new AjusteSolicitud(25L, TipoAjuste.BAJA, cantidad, "Motivo"), usuario));
        }
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(
                new AjusteSolicitud(25L, TipoAjuste.CORRECCION, new BigDecimal("-0.001"), "Motivo"), usuario));
        assertEquals(new BigDecimal("10.000"), stock.getCantidadDisponible());
        verifyNoInteractions(ajusteRepository);
        verify(stockRepository, never()).saveAndFlush(any());
    }

    @Test
    void rechazaMotivoVacioYStockInexistente() {
        for (String motivo : new String[] {null, "", "   "}) {
            assertThrows(ReglaNegocioException.class, () -> servicio.registrar(
                    new AjusteSolicitud(25L, TipoAjuste.BAJA, BigDecimal.ONE, motivo), usuario));
        }
        when(stockRepository.buscarTodosPorIdBloqueadosOrdenados(List.of(26L)))
                .thenReturn(List.of());
        assertThrows(ReglaNegocioException.class, () -> servicio.registrar(
                new AjusteSolicitud(26L, TipoAjuste.BAJA, BigDecimal.ONE, "Motivo"), usuario));
    }
}
