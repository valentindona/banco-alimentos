package com.group6.BancoAlimentos.institucion.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.institucion.dto.InstitucionRespuesta;
import com.group6.BancoAlimentos.institucion.dto.InstitucionSolicitud;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.service.ConsultaInstitucionService;
import com.group6.BancoAlimentos.institucion.service.InstitucionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instituciones")
public class InstitucionController {

    private final ConsultaInstitucionService consultaInstitucionService;
    private final InstitucionService institucionService;

    public InstitucionController(
            ConsultaInstitucionService consultaInstitucionService,
            InstitucionService institucionService) {
        this.consultaInstitucionService = consultaInstitucionService;
        this.institucionService = institucionService;
    }

    @GetMapping
    public PaginaRespuesta<InstitucionRespuesta> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<InstitucionRespuesta> resultado =
                consultaInstitucionService.listar(nombre, estadoPago, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }

    @PostMapping
    public ResponseEntity<InstitucionRespuesta> crear(@Valid @RequestBody InstitucionSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(institucionService.crear(solicitud));
    }

    @GetMapping("/{institucionId}")
    public InstitucionRespuesta consultar(@PathVariable Long institucionId) {
        return institucionService.buscarActiva(institucionId);
    }

    @PutMapping("/{institucionId}")
    public InstitucionRespuesta actualizar(
            @PathVariable Long institucionId,
            @Valid @RequestBody InstitucionSolicitud solicitud) {
        return institucionService.actualizar(institucionId, solicitud);
    }

    @DeleteMapping("/{institucionId}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long institucionId) {
        institucionService.darDeBaja(institucionId);
        return ResponseEntity.noContent().build();
    }
}
