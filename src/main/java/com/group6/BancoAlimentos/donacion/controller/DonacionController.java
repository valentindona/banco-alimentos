package com.group6.BancoAlimentos.donacion.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.donacion.dto.DonacionRespuesta;
import com.group6.BancoAlimentos.donacion.dto.DonacionSolicitud;
import com.group6.BancoAlimentos.donacion.service.ConsultaDonacionService;
import com.group6.BancoAlimentos.donacion.service.DonacionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donaciones")
public class DonacionController {

    private final DonacionService donacionService;
    private final ConsultaDonacionService consultaDonacionService;

    public DonacionController(
            DonacionService donacionService, ConsultaDonacionService consultaDonacionService) {
        this.donacionService = donacionService;
        this.consultaDonacionService = consultaDonacionService;
    }

    @GetMapping
    public PaginaRespuesta<DonacionRespuesta> listar(
            @RequestParam(required = false) Long donanteId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate vencimientoDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate vencimientoHasta,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<DonacionRespuesta> resultado = consultaDonacionService.listar(
                donanteId, fechaDesde, fechaHasta, productoId, vencimientoDesde, vencimientoHasta, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }

    @GetMapping("/{donacionId}")
    public DonacionRespuesta obtener(@PathVariable Long donacionId) {
        return consultaDonacionService.obtener(donacionId);
    }

    @PostMapping
    @Operation(summary = "Registrar una recepción, aumentar stock y emitir un comprobante")
    public ResponseEntity<DonacionRespuesta> confirmar(@Valid @RequestBody DonacionSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donacionService.confirmar(solicitud));
    }
}
