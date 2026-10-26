package com.group6.BancoAlimentos.stock.service;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.stock.dto.StockRespuesta;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.mapper.StockMapper;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaStockService {

    private final StockRepository stockRepository;
    private final StockMapper stockMapper;

    public ConsultaStockService(StockRepository stockRepository, StockMapper stockMapper) {
        this.stockRepository = stockRepository;
        this.stockMapper = stockMapper;
    }

    @Transactional(readOnly = true)
    public Page<StockRespuesta> listar(
            Long productoId, LocalDate fechaVencimiento, EstadoStock estado, int pagina, int tamano) {
        return stockRepository.buscarConFiltros(
                        productoId, fechaVencimiento, estado, Paginacion.crear(pagina, tamano))
                .map(stockMapper::aRespuesta);
    }
}
