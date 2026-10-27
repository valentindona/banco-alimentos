package com.group6.BancoAlimentos.donante.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.donante.dto.DonanteRespuesta;
import com.group6.BancoAlimentos.donante.dto.DonanteSolicitud;
import com.group6.BancoAlimentos.donante.service.ConsultaDonanteService;
import com.group6.BancoAlimentos.donante.service.DonanteService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donantes")
public class DonanteController {

    private final DonanteService donanteService;
    private final ConsultaDonanteService consultaDonanteService;

    public DonanteController(
            DonanteService donanteService, ConsultaDonanteService consultaDonanteService) {
        this.donanteService = donanteService;
        this.consultaDonanteService = consultaDonanteService;
    }

    @GetMapping
    public PaginaRespuesta<DonanteRespuesta> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<DonanteRespuesta> resultado =
                consultaDonanteService.listar(nombre, activo, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }

    @GetMapping("/{donanteId}")
    public DonanteRespuesta obtener(@PathVariable Long donanteId) {
        return consultaDonanteService.obtener(donanteId);
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
