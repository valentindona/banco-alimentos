package com.group6.BancoAlimentos.stock.mapper;

import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.stock.dto.StockRespuesta;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Component;

@Component
public class StockMapper {

    private final ProductoRepository productoRepository;

    public StockMapper(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public StockRespuesta aRespuesta(Stock stock) {
        String nombreProducto = productoRepository.findById(stock.getProductoId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "No se encontró el producto asociado al stock " + stock.getId()))
                .getNombre();
        return new StockRespuesta(
                stock.getId(),
                stock.getProductoId(),
                nombreProducto,
                stock.getFechaVencimiento(),
                stock.getCantidadDisponible(),
                stock.getEstado());
    }
}
