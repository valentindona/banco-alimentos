package com.group6.BancoAlimentos.factura.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.factura.dto.FacturaRespuesta;
import com.group6.BancoAlimentos.factura.service.ConsultaFacturaService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final ConsultaFacturaService consultaFacturaService;

    public FacturaController(ConsultaFacturaService consultaFacturaService) {
        this.consultaFacturaService = consultaFacturaService;
    }

    @GetMapping
    public PaginaRespuesta<FacturaRespuesta> listar(
            @RequestParam(required = false) Long institucionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaHasta,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<FacturaRespuesta> resultado =
                consultaFacturaService.listar(institucionId, fechaDesde, fechaHasta, estado, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }
}
