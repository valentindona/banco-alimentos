package com.group6.BancoAlimentos.detalleremito.controller;

import com.group6.BancoAlimentos.detalleremito.dto.AgregarDetalleSolicitud;
import com.group6.BancoAlimentos.detalleremito.dto.DetalleRemitoRespuesta;
import com.group6.BancoAlimentos.detalleremito.mapper.DetalleRemitoMapper;
import com.group6.BancoAlimentos.remito.service.RemitoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remitos/{remitoId}/detalles")
public class DetalleRemitoController {

    private final RemitoService remitoService;
    private final DetalleRemitoMapper detalleRemitoMapper;

    public DetalleRemitoController(RemitoService remitoService, DetalleRemitoMapper detalleRemitoMapper) {
        this.remitoService = remitoService;
        this.detalleRemitoMapper = detalleRemitoMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DetalleRemitoRespuesta agregar(
            @PathVariable Long remitoId,
            @Valid @RequestBody AgregarDetalleSolicitud solicitud) {
        return detalleRemitoMapper.aRespuesta(
                remitoService.agregarDetalle(remitoId, solicitud.stockId(), solicitud.cantidad()));
    }
}
