package com.group6.BancoAlimentos.remito.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.remito.dto.RemitoRespuesta;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.service.ConsultaRemitoService;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remitos")
public class ConsultaRemitoController {

    private final ConsultaRemitoService consultaRemitoService;

    public ConsultaRemitoController(ConsultaRemitoService consultaRemitoService) {
        this.consultaRemitoService = consultaRemitoService;
    }

    @GetMapping
    public PaginaRespuesta<RemitoRespuesta> listar(
            @RequestParam(required = false) Long institucionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaHasta,
            @RequestParam(required = false) EstadoRemito estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Page<RemitoRespuesta> resultado = consultaRemitoService.listar(
                institucionId, fechaDesde, fechaHasta, estado, pagina, tamano, usuario);
        return PaginaRespuesta.desde(resultado);
    }
}
