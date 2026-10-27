package com.group6.BancoAlimentos.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.service.ProductoService;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IngresoStockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductoService productoService;

    @Test
    void sumaIngresoAlLoteUnicoYConservaEstadoYPrecision() {
        LocalDate vencimiento = LocalDate.now().plusDays(20);
        Stock existente = Stock.builder()
                .id(5L)
                .productoId(7L)
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal("2.125"))
                .estado(EstadoStock.DISPONIBLE)
                .build();
        when(productoService.buscarModeloActivoBloqueado(7L))
                .thenReturn(Producto.builder().id(7L).nombre("Leche").tipoAlimento("Lácteo")
                        .unidadMedida("litros").activo(true).build());
        when(stockRepository.buscarPorProductoYVencimientoBloqueado(7L, vencimiento))
                .thenReturn(Optional.of(existente));
        when(stockRepository.saveAndFlush(any(Stock.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        StockService servicio = new StockService(stockRepository, productoService);

        Stock actualizado = servicio.ingresar(7L, vencimiento, new BigDecimal("0.375"));

        assertEquals(new BigDecimal("2.500"), actualizado.getCantidadDisponible());
        assertEquals(EstadoStock.DISPONIBLE, actualizado.getEstado());
        verify(stockRepository).saveAndFlush(existente);
    }

    @Test
    void creaExistenciasSeparadasPorFechaYCombinaIngresosRepetidos() {
        LocalDate vencimientoUno = LocalDate.now().plusDays(20);
        LocalDate vencimientoDos = LocalDate.now().plusDays(30);
        when(productoService.buscarModeloActivoBloqueado(7L))
                .thenReturn(Producto.builder().id(7L).nombre("Leche").tipoAlimento("Lácteo")
                        .unidadMedida("litros").activo(true).build());
        Map<LocalDate, Stock> lotes = new HashMap<>();
        when(stockRepository.buscarPorProductoYVencimientoBloqueado(eq(7L), any(LocalDate.class)))
                .thenAnswer(invocacion -> Optional.ofNullable(lotes.get(invocacion.getArgument(1))));
        when(stockRepository.saveAndFlush(any(Stock.class))).thenAnswer(invocacion -> {
            Stock stock = invocacion.getArgument(0);
            if (stock.getId() == null) {
                stock.setId(stock.getFechaVencimiento().equals(vencimientoUno) ? 10L : 11L);
            }
            lotes.put(stock.getFechaVencimiento(), stock);
            return stock;
        });
        StockService servicio = new StockService(stockRepository, productoService);

        Stock primero = servicio.ingresar(7L, vencimientoUno, new BigDecimal("1.000"));
        Stock repetido = servicio.ingresar(7L, vencimientoUno, new BigDecimal("2.000"));
        Stock segundoVencimiento = servicio.ingresar(7L, vencimientoDos, new BigDecimal("3.000"));

        assertEquals(primero.getId(), repetido.getId());
        assertEquals(new BigDecimal("3.000"), repetido.getCantidadDisponible());
        assertEquals(vencimientoUno, repetido.getFechaVencimiento());
        assertEquals(vencimientoDos, segundoVencimiento.getFechaVencimiento());
    }

    @Test
    void rechazaCantidadInvalidaVencimientoYProductoInactivo() {
        LocalDate vencimientoFuturo = LocalDate.now().plusDays(1);
        StockService servicio = new StockService(stockRepository, productoService);

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.ingresar(7L, vencimientoFuturo, BigDecimal.ZERO));
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.ingresar(7L, vencimientoFuturo, new BigDecimal("1.0001")));
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.ingresar(7L, LocalDate.now(), BigDecimal.ONE));
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.ingresar(7L, null, BigDecimal.ONE));
        when(productoService.buscarModeloActivoBloqueado(7L))
                .thenThrow(new ReglaNegocioException("Producto inactivo"));
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.ingresar(7L, vencimientoFuturo, BigDecimal.ONE));
    }
}
