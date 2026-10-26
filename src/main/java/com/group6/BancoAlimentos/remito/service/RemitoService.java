package com.group6.BancoAlimentos.remito.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemitoService {

    private final RemitoRepository remitoRepository;
    private final DetalleRemitoRepository detalleRemitoRepository;
    private final InstitucionRepository institucionRepository;
    private final StockRepository stockRepository;
    private final ProductoRepository productoRepository;

    public RemitoService(
            RemitoRepository remitoRepository,
            DetalleRemitoRepository detalleRemitoRepository,
            InstitucionRepository institucionRepository,
            StockRepository stockRepository,
            ProductoRepository productoRepository) {
        this.remitoRepository = remitoRepository;
        this.detalleRemitoRepository = detalleRemitoRepository;
        this.institucionRepository = institucionRepository;
        this.stockRepository = stockRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public Remito crearBorrador(Long institucionId, LocalDate fechaEntrega, Long creadoPorUsuarioId) {
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new ReglaNegocioException("La institución indicada no existe."));
        if (!institucion.isActiva()) {
            throw new ReglaNegocioException("La institución no está activa.");
        }
        if (institucion.getNombre() == null || institucion.getNombre().isBlank()
                || institucion.getDomicilio() == null || institucion.getDomicilio().isBlank()) {
            throw new ReglaNegocioException("La institución no tiene nombre y domicilio válidos.");
        }
        if (fechaEntrega == null || !fechaEntrega.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("La fecha de entrega debe ser posterior a hoy.");
        }
        if (creadoPorUsuarioId == null) {
            throw new ReglaNegocioException("No se pudo identificar al usuario que crea el remito.");
        }

        Remito remito = Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio(institucion.getDomicilio())
                .fechaEntrega(fechaEntrega)
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(creadoPorUsuarioId)
                .build();
        return remitoRepository.save(remito);
    }

    @Transactional
    public DetalleRemito agregarDetalle(Long remitoId, Long stockId, BigDecimal cantidad) {
        Remito remito = remitoRepository.buscarPorIdBloqueado(remitoId)
                .orElseThrow(() -> new ReglaNegocioException("El remito indicado no existe."));
        if (remito.getEstado() != EstadoRemito.BORRADOR) {
            throw new ReglaNegocioException("Solo se pueden agregar artículos a un remito en borrador.");
        }
        if (cantidad == null || cantidad.signum() <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor que cero.");
        }
        Stock stock = stockRepository.findById(stockId)
                .orElseThrow(() -> new ReglaNegocioException("La existencia seleccionada no existe."));
        if (stock.getFechaVencimiento() == null) {
            throw new ReglaNegocioException("La existencia no tiene una fecha de vencimiento registrada.");
        }
        if (!stock.getFechaVencimiento().isAfter(LocalDate.now())
                || stock.getEstado() != EstadoStock.DISPONIBLE) {
            throw new ReglaNegocioException("La existencia seleccionada está vencida o no está disponible.");
        }
        Producto producto = productoRepository.findById(stock.getProductoId())
                .orElseThrow(() -> new ReglaNegocioException("El producto de la existencia no existe."));
        if (!producto.isActivo()) {
            throw new ReglaNegocioException("El producto seleccionado no está activo.");
        }
        if (detalleRemitoRepository.existsByRemitoIdAndStockId(remitoId, stockId)) {
            throw new ReglaNegocioException("La existencia ya fue agregada a este remito.");
        }

        return detalleRemitoRepository.save(DetalleRemito.builder()
                .remitoId(remitoId)
                .stockId(stockId)
                .productoId(stock.getProductoId())
                .fechaVencimiento(stock.getFechaVencimiento())
                .productoNombre(producto.getNombre())
                .cantidad(cantidad)
                .build());
    }

    @Transactional(readOnly = true)
    public List<DetalleRemito> detallesDe(Long remitoId) {
        return detalleRemitoRepository.findByRemitoIdOrderByStockIdAsc(remitoId);
    }
}
