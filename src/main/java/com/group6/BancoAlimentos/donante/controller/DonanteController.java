package com.group6.BancoAlimentos.donante.controller;

import com.group6.BancoAlimentos.donante.dto.DonanteRespuesta;
import com.group6.BancoAlimentos.donante.dto.DonanteSolicitud;
import com.group6.BancoAlimentos.donante.service.DonanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donantes")
public class DonanteController {

    private final DonanteService donanteService;

    public DonanteController(DonanteService donanteService) {
        this.donanteService = donanteService;
    }

    @PostMapping
    public ResponseEntity<DonanteRespuesta> crear(@Valid @RequestBody DonanteSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(donanteService.crear(solicitud));
    }

    @PutMapping("/{donanteId}")
    public DonanteRespuesta actualizar(
            @PathVariable Long donanteId, @Valid @RequestBody DonanteSolicitud solicitud) {
        return donanteService.actualizar(donanteId, solicitud);
    }

    @DeleteMapping("/{donanteId}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long donanteId) {
        donanteService.darDeBaja(donanteId);
        return ResponseEntity.noContent().build();
    }
}
