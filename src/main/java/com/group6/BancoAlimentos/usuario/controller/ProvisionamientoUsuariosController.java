package com.group6.BancoAlimentos.usuario.controller;

import com.group6.BancoAlimentos.usuario.dto.AltaUsuarioSolicitud;
import com.group6.BancoAlimentos.usuario.dto.UsuarioRespuesta;
import com.group6.BancoAlimentos.usuario.service.ProvisionamientoUsuariosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios")
public class ProvisionamientoUsuariosController {

    private final ProvisionamientoUsuariosService provisionamientoUsuariosService;

    public ProvisionamientoUsuariosController(ProvisionamientoUsuariosService provisionamientoUsuariosService) {
        this.provisionamientoUsuariosService = provisionamientoUsuariosService;
    }

    @PostMapping
    @Operation(summary = "Provisionar un usuario; requiere el rol user_deposito")
    public ResponseEntity<UsuarioRespuesta> crear(@Valid @RequestBody AltaUsuarioSolicitud solicitud) {
        UsuarioRespuesta respuesta = provisionamientoUsuariosService.crear(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(respuesta);
    }
}
