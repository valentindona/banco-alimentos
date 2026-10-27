package com.group6.BancoAlimentos.donacion.controller;

import com.group6.BancoAlimentos.donacion.dto.DonacionRespuesta;
import com.group6.BancoAlimentos.donacion.dto.DonacionSolicitud;
import com.group6.BancoAlimentos.donacion.service.DonacionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donaciones")
public class DonacionController {

    private final DonacionService donacionService;

    public DonacionController(DonacionService donacionService) {
        this.donacionService = donacionService;
    }

    @PostMapping
    @Operation(summary = "Registrar una recepción, aumentar stock y emitir un comprobante")
    public ResponseEntity<DonacionRespuesta> confirmar(@Valid @RequestBody DonacionSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donacionService.confirmar(solicitud));
    }
}
