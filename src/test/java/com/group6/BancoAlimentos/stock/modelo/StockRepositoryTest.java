package com.group6.BancoAlimentos.stock.modelo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class StockRepositoryTest {

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void admiteVencimientoNuloYRequiereCantidadNoNegativaYEstadoValido() {
        for (EstadoStock estado : EstadoStock.values()) {
            Producto producto = productoRepository.saveAndFlush(Producto.builder()
                    .nombre("Lentejas " + estado)
                    .unidadMedida("kg")
                    .activo(true)
                    .build());
            Stock stock = crearStock(producto.getId(), null, estado, BigDecimal.ZERO);
            assertTrue(stockRepository.saveAndFlush(stock).getId() > 0);
        }

        Producto productoConCantidadNegativa = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Lentejas negativas")
                .unidadMedida("kg")
                .activo(true)
                .build());
        assertThrows(RuntimeException.class, () -> stockRepository.saveAndFlush(
                crearStock(productoConCantidadNegativa.getId(), LocalDate.now().plusDays(10), EstadoStock.DISPONIBLE,
                        new BigDecimal("-0.001"))));
    }

    @Test
    void impideDosExistenciasDelMismoProductoYVencimiento() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Harina")
                .unidadMedida("kg")
                .activo(true)
                .build());
        LocalDate vencimiento = LocalDate.now().plusDays(30);
        stockRepository.saveAndFlush(crearStock(producto.getId(), vencimiento, EstadoStock.DISPONIBLE,
                new BigDecimal("10.000")));

        assertThrows(DataIntegrityViolationException.class,
                () -> stockRepository.saveAndFlush(crearStock(producto.getId(), vencimiento,
                        EstadoStock.DISPONIBLE, new BigDecimal("2.000"))));
    }

    private Stock crearStock(Long productoId, LocalDate vencimiento, EstadoStock estado, BigDecimal cantidad) {
        return Stock.builder()
                .productoId(productoId)
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(cantidad)
                .estado(estado)
                .build();
    }
}
