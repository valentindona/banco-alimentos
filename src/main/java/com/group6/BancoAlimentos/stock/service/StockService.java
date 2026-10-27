package com.group6.BancoAlimentos.stock.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.service.ProductoService;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockService {

    private final StockRepository stockRepository;
    private final ProductoService productoService;

    public StockService(StockRepository stockRepository, ProductoService productoService) {
        this.stockRepository = stockRepository;
        this.productoService = productoService;
    }

    @Transactional
    public Stock ingresar(Long productoId, LocalDate fechaVencimiento, BigDecimal cantidad) {
        validarIngreso(fechaVencimiento, cantidad);
        Producto producto = productoService.buscarModeloActivoBloqueado(productoId);
        Stock stock = stockRepository
                .buscarPorProductoYVencimientoBloqueado(productoId, fechaVencimiento)
                .orElseGet(() -> Stock.builder()
                        .productoId(producto.getId())
                        .fechaVencimiento(fechaVencimiento)
                        .cantidadDisponible(BigDecimal.ZERO.setScale(3))
                        .estado(EstadoStock.DISPONIBLE)
                        .build());
        stock.setCantidadDisponible(stock.getCantidadDisponible().add(cantidad).setScale(3));
        if (stock.getEstado() == null) {
            stock.setEstado(EstadoStock.DISPONIBLE);
        }
        return stockRepository.saveAndFlush(stock);
    }

    private void validarIngreso(LocalDate fechaVencimiento, BigDecimal cantidad) {
        if (fechaVencimiento == null || !fechaVencimiento.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("El vencimiento del ingreso debe ser posterior a hoy");
        }
        if (cantidad == null || cantidad.signum() <= 0 || cantidad.scale() > 3) {
            throw new ReglaNegocioException("La cantidad ingresada debe ser positiva y tener hasta tres decimales");
        }
    }
}
