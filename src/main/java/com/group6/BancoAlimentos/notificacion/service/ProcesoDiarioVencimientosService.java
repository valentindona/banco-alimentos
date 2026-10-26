package com.group6.BancoAlimentos.notificacion.service;

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
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProcesoDiarioVencimientosService {

    private final StockRepository stockRepository;
    private final ProductoRepository productoRepository;
    private final NotificacionRepository notificacionRepository;
    private final VencimientoStockService vencimientoStockService;

    public ProcesoDiarioVencimientosService(
            StockRepository stockRepository,
            ProductoRepository productoRepository,
            NotificacionRepository notificacionRepository,
            VencimientoStockService vencimientoStockService) {
        this.stockRepository = stockRepository;
        this.productoRepository = productoRepository;
        this.notificacionRepository = notificacionRepository;
        this.vencimientoStockService = vencimientoStockService;
    }

    @Transactional
    public void procesar(LocalDate fechaNegocio) {
        List<Stock> vencidos = stockRepository
                .findByFechaVencimientoLessThanEqualAndCantidadDisponibleGreaterThanOrderByIdAsc(
                        fechaNegocio, BigDecimal.ZERO);
        List<Stock> proximos = stockRepository
                .findByFechaVencimientoBetweenAndCantidadDisponibleGreaterThanOrderByIdAsc(
                        fechaNegocio.plusDays(1), fechaNegocio.plusDays(3), BigDecimal.ZERO);
        List<Stock> actualizados = new ArrayList<>();

        for (Stock stock : vencidos) {
            vencimientoStockService.actualizarEstado(stock, fechaNegocio);
            if (stock.getEstado() != EstadoStock.VENCIDO) {
                throw new IllegalStateException("El stock vencido no quedó marcado como vencido.");
            }
            actualizados.add(stock);
            crearNotificacionSiFalta(stock, TipoNotificacion.VENCIDO);
        }
        for (Stock stock : proximos) {
            vencimientoStockService.actualizarEstado(stock, fechaNegocio);
            if (!vencimientoStockService.estaProximoAVencer(stock, fechaNegocio)) {
                continue;
            }
            actualizados.add(stock);
            crearNotificacionSiFalta(stock, TipoNotificacion.POR_VENCER);
        }

        if (!actualizados.isEmpty()) {
            stockRepository.saveAll(actualizados);
        }
    }

    private void crearNotificacionSiFalta(Stock stock, TipoNotificacion tipo) {
        if (notificacionRepository.existsByStockIdAndTipo(stock.getId(), tipo)) {
            return;
        }
        Producto producto = productoRepository.findById(stock.getProductoId())
                .orElseThrow(() -> new IllegalStateException(
                        "No se encontró el producto asociado al stock " + stock.getId() + "."));
        String mensaje = tipo == TipoNotificacion.VENCIDO
                ? String.format(
                        "El producto %s venció el %s; quedan %s %s disponibles.",
                        producto.getNombre(),
                        stock.getFechaVencimiento(),
                        stock.getCantidadDisponible().toPlainString(),
                        producto.getUnidadMedida())
                : String.format(
                        "El producto %s vence el %s; quedan %s %s disponibles.",
                        producto.getNombre(),
                        stock.getFechaVencimiento(),
                        stock.getCantidadDisponible().toPlainString(),
                        producto.getUnidadMedida());
        notificacionRepository.save(Notificacion.builder()
                .stockId(stock.getId())
                .tipo(tipo)
                .audiencia("user_deposito")
                .mensaje(mensaje)
                .build());
    }
}
