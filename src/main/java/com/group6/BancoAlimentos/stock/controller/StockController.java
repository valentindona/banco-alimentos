package com.group6.BancoAlimentos.stock.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.stock.dto.StockRespuesta;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.service.ConsultaStockService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final ConsultaStockService consultaStockService;

    public StockController(ConsultaStockService consultaStockService) {
        this.consultaStockService = consultaStockService;
    }

    @GetMapping
    public PaginaRespuesta<StockRespuesta> listar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaVencimiento,
            @RequestParam(required = false) EstadoStock estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<StockRespuesta> resultado =
                consultaStockService.listar(productoId, fechaVencimiento, estado, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }
}
