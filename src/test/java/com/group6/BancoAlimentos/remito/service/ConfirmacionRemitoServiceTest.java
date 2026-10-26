package com.group6.BancoAlimentos.remito.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConfirmacionRemitoServiceTest {

    @Mock
    private RemitoRepository remitoRepository;

    @Mock
    private DetalleRemitoRepository detalleRepository;

    @Mock
    private StockRepository stockRepository;

    @InjectMocks
    private ConfirmacionRemitoService servicio;

    private Remito remito;
    private Stock stockUno;
    private Stock stockDos;
    private DetalleRemito detalleUno;
    private DetalleRemito detalleDos;

    @BeforeEach
    void prepararDatos() {
        remito = Remito.builder()
                .id(13L)
                .institucionId(5L)
                .institucionNombre("Institución")
                .institucionDomicilio("Calle 123")
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(41L)
                .build();
        stockUno = crearStock(11L, "10.000", LocalDate.now().plusDays(20));
        stockDos = crearStock(12L, "8.000", LocalDate.now().plusDays(30));
        detalleUno = crearDetalle(11L, "2.000");
        detalleDos = crearDetalle(12L, "3.000");
        detalleDos.setFechaVencimiento(stockDos.getFechaVencimiento());
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));
    }

    @Test
    void validaTodasLasLineasAntesDeDescontarStockInsuficiente() {
        when(detalleRepository.findByRemitoIdOrderByStockIdAsc(13L)).thenReturn(List.of(detalleUno, detalleDos));
        stockDos.setCantidadDisponible(new BigDecimal("2.000"));
        when(stockRepository.buscarTodosPorIdBloqueadosOrdenados(List.of(11L, 12L)))
                .thenReturn(List.of(stockUno, stockDos));

        assertThrows(ReglaNegocioException.class, () -> servicio.confirmar(13L));

        assertEquals(new BigDecimal("10.000"), stockUno.getCantidadDisponible());
        assertEquals(new BigDecimal("2.000"), stockDos.getCantidadDisponible());
        verify(stockRepository, never()).saveAll(anyList());
    }

    @Test
    void revalidaVencimientoAlConfirmarYNoDescuentaNingunaLinea() {
        when(detalleRepository.findByRemitoIdOrderByStockIdAsc(13L)).thenReturn(List.of(detalleUno, detalleDos));
        stockDos.setFechaVencimiento(LocalDate.now());
        when(stockRepository.buscarTodosPorIdBloqueadosOrdenados(List.of(11L, 12L)))
                .thenReturn(List.of(stockUno, stockDos));

        assertThrows(ReglaNegocioException.class, () -> servicio.confirmar(13L));

        assertEquals(new BigDecimal("10.000"), stockUno.getCantidadDisponible());
        assertEquals(new BigDecimal("8.000"), stockDos.getCantidadDisponible());
        verify(stockRepository, never()).saveAll(anyList());
    }

    @Test
    void descuentaLasCantidadesExactasYConfirmaUnaSolaVez() {
        when(detalleRepository.findByRemitoIdOrderByStockIdAsc(13L)).thenReturn(List.of(detalleUno, detalleDos));
        when(stockRepository.buscarTodosPorIdBloqueadosOrdenados(List.of(11L, 12L)))
                .thenReturn(List.of(stockUno, stockDos));
        when(stockRepository.saveAll(anyList())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(remitoRepository.save(any(Remito.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Remito confirmado = servicio.confirmar(13L);

        assertEquals(new BigDecimal("8.000"), stockUno.getCantidadDisponible());
        assertEquals(new BigDecimal("5.000"), stockDos.getCantidadDisponible());
        assertEquals(EstadoRemito.CONFIRMADO, confirmado.getEstado());
        verify(stockRepository).saveAll(anyList());
    }

    @Test
    void noPermiteConfirmarDosVeces() {
        remito.setEstado(EstadoRemito.CONFIRMADO);

        assertThrows(ReglaNegocioException.class, () -> servicio.confirmar(13L));

        verify(stockRepository, never()).buscarTodosPorIdBloqueadosOrdenados(anyList());
    }

    @Test
    void rechazaRemitoVacioSinModificarElEstado() {
        when(detalleRepository.findByRemitoIdOrderByStockIdAsc(13L)).thenReturn(List.of());

        assertThrows(ReglaNegocioException.class, () -> servicio.confirmar(13L));

        assertEquals(EstadoRemito.BORRADOR, remito.getEstado());
        verify(stockRepository, never()).buscarTodosPorIdBloqueadosOrdenados(anyList());
    }

    private Stock crearStock(Long id, String cantidad, LocalDate vencimiento) {
        return Stock.builder()
                .id(id)
                .productoId(id + 100)
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal(cantidad))
                .estado(EstadoStock.DISPONIBLE)
                .build();
    }

    private DetalleRemito crearDetalle(Long stockId, String cantidad) {
        return DetalleRemito.builder()
                .id(stockId + 1000)
                .remitoId(13L)
                .stockId(stockId)
                .productoId(stockId + 100)
                .fechaVencimiento(LocalDate.now().plusDays(20))
                .productoNombre("Producto " + stockId)
                .cantidad(new BigDecimal(cantidad))
                .build();
    }
}
