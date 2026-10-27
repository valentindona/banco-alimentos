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
        Institucion institucion = validarInstitucionEFecha(institucionId, fechaEntrega);
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
    public Remito actualizarBorrador(Long remitoId, Long institucionId, LocalDate fechaEntrega) {
        Remito remito = obtenerBorradorBloqueado(remitoId);
        Institucion institucion = validarInstitucionEFecha(institucionId, fechaEntrega);
        remito.setInstitucionId(institucion.getId());
        remito.setInstitucionNombre(institucion.getNombre());
        remito.setInstitucionDomicilio(institucion.getDomicilio());
        remito.setFechaEntrega(fechaEntrega);
        return remitoRepository.save(remito);
    }

    @Transactional
    public DetalleRemito agregarDetalle(Long remitoId, Long stockId, BigDecimal cantidad) {
        obtenerBorradorBloqueado(remitoId);
        Stock stock = validarDetalle(remitoId, stockId, cantidad, null);
        Producto producto = productoRepository.findById(stock.getProductoId())
                .orElseThrow(() -> new ReglaNegocioException("El producto de la existencia no existe."));

        return detalleRemitoRepository.save(DetalleRemito.builder()
                .remitoId(remitoId)
                .stockId(stockId)
                .productoId(stock.getProductoId())
                .fechaVencimiento(stock.getFechaVencimiento())
                .productoNombre(producto.getNombre())
                .cantidad(cantidad)
                .build());
    }

    @Transactional
    public DetalleRemito actualizarDetalle(Long remitoId, Long detalleId, Long stockId, BigDecimal cantidad) {
        obtenerBorradorBloqueado(remitoId);
        DetalleRemito detalle = detalleRemitoRepository.findByIdAndRemitoId(detalleId, remitoId)
                .orElseThrow(() -> new ReglaNegocioException("El artículo indicado no existe en este remito."));
        Stock stock = validarDetalle(remitoId, stockId, cantidad, detalleId);
        Producto producto = productoRepository.findById(stock.getProductoId())
                .orElseThrow(() -> new ReglaNegocioException("El producto de la existencia no existe."));
        detalle.setStockId(stock.getId());
        detalle.setProductoId(stock.getProductoId());
        detalle.setFechaVencimiento(stock.getFechaVencimiento());
        detalle.setProductoNombre(producto.getNombre());
        detalle.setCantidad(cantidad);
        return detalleRemitoRepository.save(detalle);
    }

    @Transactional
    public void eliminarDetalle(Long remitoId, Long detalleId) {
        obtenerBorradorBloqueado(remitoId);
        DetalleRemito detalle = detalleRemitoRepository.findByIdAndRemitoId(detalleId, remitoId)
                .orElseThrow(() -> new ReglaNegocioException("El artículo indicado no existe en este remito."));
        detalleRemitoRepository.delete(detalle);
    }

    @Transactional
    public void eliminarBorrador(Long remitoId) {
        Remito remito = obtenerBorradorBloqueado(remitoId);
        detalleRemitoRepository.deleteAll(detalleRemitoRepository.findByRemitoIdOrderByStockIdAsc(remitoId));
        remitoRepository.delete(remito);
    }

    @Transactional(readOnly = true)
    public List<DetalleRemito> detallesDe(Long remitoId) {
        return detalleRemitoRepository.findByRemitoIdOrderByStockIdAsc(remitoId);
    }

    private Remito obtenerBorradorBloqueado(Long remitoId) {
        Remito remito = remitoRepository.buscarPorIdBloqueado(remitoId)
                .orElseThrow(() -> new ReglaNegocioException("El remito indicado no existe."));
        if (remito.getEstado() != EstadoRemito.BORRADOR) {
            throw new ReglaNegocioException("Solo se pueden modificar remitos en borrador.");
        }
        return remito;
    }

    private Stock validarDetalle(Long remitoId, Long stockId, BigDecimal cantidad, Long detalleId) {
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
        boolean stockYaIncluido = detalleRemitoRepository.findByRemitoIdOrderByStockIdAsc(remitoId).stream()
                .anyMatch(detalle -> detalle.getStockId().equals(stockId)
                        && !detalle.getId().equals(detalleId));
        if (stockYaIncluido) {
            throw new ReglaNegocioException("La existencia ya fue agregada a este remito.");
        }
        return stock;
    }

    private Institucion validarInstitucionEFecha(Long institucionId, LocalDate fechaEntrega) {
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
        return institucion;
    }
}
