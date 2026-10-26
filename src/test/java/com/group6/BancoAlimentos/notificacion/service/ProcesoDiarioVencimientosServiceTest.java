package com.group6.BancoAlimentos.notificacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.group6.BancoAlimentos.notificacion.modelo.Notificacion;
import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import com.group6.BancoAlimentos.notificacion.repository.NotificacionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import com.group6.BancoAlimentos.stock.service.VencimientoStockService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcesoDiarioVencimientosServiceTest {

    private final LocalDate fechaNegocio = LocalDate.of(2026, 10, 26);

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private NotificacionRepository notificacionRepository;

    @Spy
    private VencimientoStockService vencimientoStockService;

    @InjectMocks
    private ProcesoDiarioVencimientosService servicio;

    @BeforeEach
    void prepararProductos() {
        lenient().when(productoRepository.findById(1L)).thenReturn(Optional.of(
                Producto.builder().id(1L).nombre("Arroz").unidadMedida("kg").activo(true).build()));
        lenient().when(productoRepository.findById(2L)).thenReturn(Optional.of(
                Producto.builder().id(2L).nombre("Lentejas").unidadMedida("bolsa").activo(true).build()));
    }

    @Test
    void recuperaVencidosAtrasadosYAlertaSoloStockPositivoHastaTresDias() {
        Stock vencioHoy = crearStock(11L, 1L, fechaNegocio, "4.000", EstadoStock.DISPONIBLE);
        Stock vencioAyer = crearStock(12L, 1L, fechaNegocio.minusDays(4), "2.000", EstadoStock.DISPONIBLE);
        Stock venceEnTresDias = crearStock(13L, 2L, fechaNegocio.plusDays(3), "8.000", EstadoStock.DISPONIBLE);
        Stock venceEnCuatroDias = crearStock(14L, 2L, fechaNegocio.plusDays(4), "8.000", EstadoStock.DISPONIBLE);
        Stock cantidadCero = crearStock(15L, 1L, fechaNegocio, "0.000", EstadoStock.DISPONIBLE);
        when(stockRepository.findByFechaVencimientoLessThanEqualAndCantidadDisponibleGreaterThanOrderByIdAsc(
                fechaNegocio, BigDecimal.ZERO))
                .thenReturn(List.of(vencioHoy, vencioAyer));
        when(stockRepository.findByFechaVencimientoBetweenAndCantidadDisponibleGreaterThanOrderByIdAsc(
                fechaNegocio.plusDays(1), fechaNegocio.plusDays(3), BigDecimal.ZERO))
                .thenReturn(List.of(venceEnTresDias));
        when(notificacionRepository.existsByStockIdAndTipo(any(), any())).thenReturn(false);
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> {
            Notificacion notificacion = invocacion.getArgument(0);
            notificacion.setId(100L);
            return notificacion;
        });

        servicio.procesar(fechaNegocio);

        assertEquals(EstadoStock.VENCIDO, vencioHoy.getEstado());
        assertEquals(EstadoStock.VENCIDO, vencioAyer.getEstado());
        assertEquals(EstadoStock.DISPONIBLE, venceEnTresDias.getEstado());
        assertEquals(EstadoStock.DISPONIBLE, venceEnCuatroDias.getEstado());
        assertEquals(EstadoStock.DISPONIBLE, cantidadCero.getEstado());
        verify(notificacionRepository).save(argThat(notificacion ->
                notificacion.getStockId().equals(11L)
                        && notificacion.getTipo() == TipoNotificacion.VENCIDO
                        && notificacion.getMensaje().contains("Arroz")
                        && notificacion.getMensaje().contains("4.000")
                        && notificacion.getMensaje().contains(fechaNegocio.toString())));
        verify(notificacionRepository).save(argThat(notificacion ->
                notificacion.getStockId().equals(12L)
                        && notificacion.getTipo() == TipoNotificacion.VENCIDO));
        verify(notificacionRepository).save(argThat(notificacion ->
                notificacion.getStockId().equals(13L)
                        && notificacion.getTipo() == TipoNotificacion.POR_VENCER
                        && notificacion.getMensaje().contains("Lentejas")));
        verify(notificacionRepository, never()).save(argThat(notificacion ->
                notificacion.getStockId().equals(14L)
                        || notificacion.getStockId().equals(15L)));
    }

    @Test
    void ejecucionesRepetidasNoDuplicanAvisosYCompletaAvisoDeStockYaVencido() {
        Stock vencido = crearStock(21L, 1L, fechaNegocio.minusDays(1), "1.000", EstadoStock.VENCIDO);
        when(stockRepository.findByFechaVencimientoLessThanEqualAndCantidadDisponibleGreaterThanOrderByIdAsc(
                fechaNegocio, BigDecimal.ZERO))
                .thenReturn(List.of(vencido));
        when(stockRepository.findByFechaVencimientoBetweenAndCantidadDisponibleGreaterThanOrderByIdAsc(
                fechaNegocio.plusDays(1), fechaNegocio.plusDays(3), BigDecimal.ZERO))
                .thenReturn(List.of());
        when(notificacionRepository.existsByStockIdAndTipo(21L, TipoNotificacion.VENCIDO))
                .thenReturn(false, true);
        when(notificacionRepository.save(any(Notificacion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        servicio.procesar(fechaNegocio);
        servicio.procesar(fechaNegocio);

        verify(notificacionRepository).save(argThat(notificacion ->
                notificacion.getStockId().equals(21L)
                        && notificacion.getTipo() == TipoNotificacion.VENCIDO));
        assertEquals(EstadoStock.VENCIDO, vencido.getEstado());
    }

    @Test
    void incluyeElPrimerDiaEnAvisosYNoCreaAvisosSiYaExisten() {
        Stock venceMañana = crearStock(31L, 1L, fechaNegocio.plusDays(1), "2.000", EstadoStock.DISPONIBLE);
        when(stockRepository.findByFechaVencimientoLessThanEqualAndCantidadDisponibleGreaterThanOrderByIdAsc(
                fechaNegocio, BigDecimal.ZERO))
                .thenReturn(List.of());
        when(stockRepository.findByFechaVencimientoBetweenAndCantidadDisponibleGreaterThanOrderByIdAsc(
                fechaNegocio.plusDays(1), fechaNegocio.plusDays(3), BigDecimal.ZERO))
                .thenReturn(List.of(venceMañana));
        when(notificacionRepository.existsByStockIdAndTipo(31L, TipoNotificacion.POR_VENCER)).thenReturn(true);

        servicio.procesar(fechaNegocio);

        verify(notificacionRepository, never()).save(any(Notificacion.class));
    }

    @Test
    void programadorUsaLaFechaDelRelojEnLaZonaDeNegocio() {
        Clock reloj = Clock.fixed(
                Instant.parse("2026-10-27T02:30:00Z"),
                ZoneId.of("America/Argentina/Buenos_Aires"));
        ProgramadorVencimientos programador = new ProgramadorVencimientos(servicio, reloj);

        programador.procesarVencimientosDiarios();

        verify(stockRepository)
                .findByFechaVencimientoLessThanEqualAndCantidadDisponibleGreaterThanOrderByIdAsc(
                        LocalDate.of(2026, 10, 26), BigDecimal.ZERO);
        verify(stockRepository)
                .findByFechaVencimientoBetweenAndCantidadDisponibleGreaterThanOrderByIdAsc(
                        LocalDate.of(2026, 10, 27), LocalDate.of(2026, 10, 29), BigDecimal.ZERO);
    }

    private Stock crearStock(Long id, Long productoId, LocalDate vencimiento, String cantidad, EstadoStock estado) {
        return Stock.builder()
                .id(id)
                .productoId(productoId)
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal(cantidad))
                .estado(estado)
                .build();
    }
}
