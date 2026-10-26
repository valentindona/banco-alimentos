package com.group6.BancoAlimentos.remito.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfirmacionRemitoService {

    private final RemitoRepository remitoRepository;
    private final DetalleRemitoRepository detalleRemitoRepository;
    private final StockRepository stockRepository;

    public ConfirmacionRemitoService(
            RemitoRepository remitoRepository,
            DetalleRemitoRepository detalleRemitoRepository,
            StockRepository stockRepository) {
        this.remitoRepository = remitoRepository;
        this.detalleRemitoRepository = detalleRemitoRepository;
        this.stockRepository = stockRepository;
    }

    @Transactional
    public Remito confirmar(Long remitoId) {
        Remito remito = remitoRepository.buscarPorIdBloqueado(remitoId)
                .orElseThrow(() -> new ReglaNegocioException("El remito indicado no existe."));
        if (remito.getEstado() != EstadoRemito.BORRADOR) {
            throw new ReglaNegocioException("Solo se puede confirmar un remito en borrador.");
        }

        List<DetalleRemito> detalles = detalleRemitoRepository.findByRemitoIdOrderByStockIdAsc(remitoId);
        if (detalles.isEmpty()) {
            throw new ReglaNegocioException("No se puede confirmar un remito sin artículos.");
        }
        List<Long> idsStock = new TreeSet<>(detalles.stream().map(DetalleRemito::getStockId).toList())
                .stream()
                .toList();
        List<Stock> existencias = stockRepository.buscarTodosPorIdBloqueadosOrdenados(idsStock);
        if (existencias.size() != idsStock.size()) {
            throw new ReglaNegocioException("Una o más existencias del remito ya no están disponibles.");
        }
        Map<Long, Stock> existenciasPorId = new HashMap<>();
        for (Stock stock : existencias) {
            existenciasPorId.put(stock.getId(), stock);
        }

        LocalDate fechaNegocio = LocalDate.now();
        for (DetalleRemito detalle : detalles) {
            Stock stock = existenciasPorId.get(detalle.getStockId());
            if (stock == null || stock.getEstado() != EstadoStock.DISPONIBLE
                    || stock.getFechaVencimiento() == null
                    || !stock.getFechaVencimiento().isAfter(fechaNegocio)
                    || !stock.getFechaVencimiento().equals(detalle.getFechaVencimiento())) {
                throw new ReglaNegocioException(
                        "La existencia seleccionada está vencida o ya no está disponible.");
            }
            if (detalle.getCantidad() == null || detalle.getCantidad().signum() <= 0
                    || stock.getCantidadDisponible().compareTo(detalle.getCantidad()) < 0) {
                throw new ReglaNegocioException(
                        "La cantidad solicitada supera la existencia disponible.");
            }
        }

        for (DetalleRemito detalle : detalles) {
            Stock stock = existenciasPorId.get(detalle.getStockId());
            stock.setCantidadDisponible(stock.getCantidadDisponible().subtract(detalle.getCantidad()));
        }
        stockRepository.saveAll(existencias);
        remito.setEstado(EstadoRemito.CONFIRMADO);
        remito.setFechaConfirmacion(Instant.now());
        return remitoRepository.save(remito);
    }
}
