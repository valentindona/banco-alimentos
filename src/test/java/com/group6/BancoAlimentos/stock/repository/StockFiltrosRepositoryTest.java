package com.group6.BancoAlimentos.stock.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class StockFiltrosRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Test
    void consultaExistenciasPorIdProductoYVencimientoYBuscaEntidadesPorId() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Fideos")
                .unidadMedida("caja")
                .activo(true)
                .build());
        Institucion institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Comedor")
                .domicilio("Av. Ejemplo 10")
                .estadoPago(EstadoPago.EN_DEUDA)
                .activa(true)
                .build());
        LocalDate vencimiento = LocalDate.now().plusDays(12);
        Stock stock = stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal("3.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());

        assertEquals(stock.getId(), stockRepository.findById(stock.getId()).orElseThrow().getId());
        assertEquals(stock.getId(), stockRepository.findByProductoIdAndFechaVencimiento(
                producto.getId(), vencimiento).orElseThrow().getId());
        assertEquals(producto.getId(), productoRepository.findById(producto.getId()).orElseThrow().getId());
        assertEquals(institucion.getId(), institucionRepository.findById(institucion.getId()).orElseThrow().getId());
    }
}
