package com.group6.BancoAlimentos.stock.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.mapper.StockMapper;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultaStockServiceTest {

    @Mock
    private StockRepository stockRepository;

    @Mock
    private StockMapper stockMapper;

    @Test
    void filtraPorProductoVencimientoEstadoYPagina() {
        ConsultaStockService servicio = new ConsultaStockService(stockRepository, stockMapper);
        LocalDate vencimiento = LocalDate.of(2026, 12, 1);
        Stock stock = Stock.builder()
                .id(6L)
                .productoId(4L)
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal("8.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build();
        when(stockRepository.buscarConFiltros(4L, vencimiento, EstadoStock.DISPONIBLE,
                        PageRequest.of(0, 5)))
                .thenReturn(new PageImpl<>(List.of(stock), PageRequest.of(0, 5), 1));
        when(stockMapper.aRespuesta(stock)).thenReturn(
                new com.group6.BancoAlimentos.stock.dto.StockRespuesta(
                        6L, 4L, "Arroz", vencimiento, new BigDecimal("8.000"), EstadoStock.DISPONIBLE));

        var pagina = servicio.listar(4L, vencimiento, EstadoStock.DISPONIBLE, 0, 5);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(6L, pagina.getContent().get(0).id());
        verify(stockRepository).buscarConFiltros(
                4L, vencimiento, EstadoStock.DISPONIBLE, PageRequest.of(0, 5));
    }
}
