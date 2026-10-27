package com.group6.BancoAlimentos.remito.controller;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.detalleremito.mapper.DetalleRemitoMapper;
import com.group6.BancoAlimentos.remito.dto.CrearRemitoSolicitud;
import com.group6.BancoAlimentos.remito.dto.RemitoRespuesta;
import com.group6.BancoAlimentos.remito.mapper.RemitoMapper;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.service.ConfirmacionRemitoService;
import com.group6.BancoAlimentos.remito.service.RemitoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/remitos")
public class RemitoController {

    private final RemitoService remitoService;
    private final ConfirmacionRemitoService confirmacionRemitoService;
    private final RemitoMapper remitoMapper;
    private final DetalleRemitoMapper detalleRemitoMapper;

    public RemitoController(
            RemitoService remitoService,
            ConfirmacionRemitoService confirmacionRemitoService,
            RemitoMapper remitoMapper,
            DetalleRemitoMapper detalleRemitoMapper) {
        this.remitoService = remitoService;
        this.confirmacionRemitoService = confirmacionRemitoService;
        this.remitoMapper = remitoMapper;
        this.detalleRemitoMapper = detalleRemitoMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RemitoRespuesta crearBorrador(
            @Valid @RequestBody CrearRemitoSolicitud solicitud,
            @AuthenticationPrincipal UsuarioAutenticado usuario) {
        Remito remito = remitoService.crearBorrador(
                solicitud.institucionId(), solicitud.fechaEntrega(), usuario.usuarioId());
        return mapear(remito);
    }

    @PutMapping("/{remitoId}")
    public RemitoRespuesta actualizarBorrador(
            @PathVariable Long remitoId,
            @Valid @RequestBody CrearRemitoSolicitud solicitud) {
        return mapear(remitoService.actualizarBorrador(
                remitoId, solicitud.institucionId(), solicitud.fechaEntrega()));
    }

    @DeleteMapping("/{remitoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminarBorrador(@PathVariable Long remitoId) {
        remitoService.eliminarBorrador(remitoId);
    }

    @PostMapping("/{remitoId}/confirmacion")
    public RemitoRespuesta confirmar(@PathVariable Long remitoId) {
        return mapear(confirmacionRemitoService.confirmar(remitoId));
    }

    private RemitoRespuesta mapear(Remito remito) {
        return remitoMapper.aRespuesta(
                remito,
                remitoService.detallesDe(remito.getId()).stream()
                        .map(detalleRemitoMapper::aRespuesta)
                        .toList());
    }
}
