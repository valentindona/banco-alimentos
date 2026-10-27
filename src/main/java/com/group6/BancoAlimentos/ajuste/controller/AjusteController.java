package com.group6.BancoAlimentos.ajuste.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.ajuste.dto.AjusteRespuesta;
import com.group6.BancoAlimentos.ajuste.dto.AjusteSolicitud;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import com.group6.BancoAlimentos.ajuste.service.AjusteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ajustes")
@Tag(name = "Ajustes de stock")
public class AjusteController {

    private final AjusteService ajusteService;

    public AjusteController(AjusteService ajusteService) {
        this.ajusteService = ajusteService;
    }

    @PostMapping
    @Operation(summary = "Registrar una corrección o baja de un lote")
    public ResponseEntity<AjusteRespuesta> registrar(
            @Valid @RequestBody AjusteSolicitud solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ajusteService.registrar(solicitud, usuario));
    }

    @GetMapping
    @Operation(summary = "Consultar el historial paginado de ajustes")
    public PaginaRespuesta<AjusteRespuesta> listar(
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaVencimiento,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaHasta,
            @RequestParam(required = false) Long usuarioId,
            @RequestParam(required = false) TipoAjuste tipo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<AjusteRespuesta> resultado = ajusteService.listar(
                productoId, fechaVencimiento, fechaDesde, fechaHasta, usuarioId, tipo, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }
}
