package com.group6.BancoAlimentos.usuario.controller;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.usuario.dto.LoginSolicitud;
import com.group6.BancoAlimentos.usuario.dto.TokenRespuesta;
import com.group6.BancoAlimentos.usuario.dto.UsuarioRespuesta;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.service.AutenticacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticacion")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;

    public AutenticacionController(AutenticacionService autenticacionService) {
        this.autenticacionService = autenticacionService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión y obtener un token JWT")
    public ResponseEntity<TokenRespuesta> iniciarSesion(@Valid @RequestBody LoginSolicitud solicitud) {
        return ResponseEntity.ok(autenticacionService.iniciarSesion(solicitud));
    }

    @GetMapping("/yo")
    @Operation(summary = "Consultar la identidad autenticada")
    public UsuarioRespuesta consultarIdentidad(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        RolUsuario rol = RolUsuario.valueOf(usuario.roles().getFirst().toUpperCase(java.util.Locale.ROOT));
        return new UsuarioRespuesta(
                usuario.usuarioId(),
                usuario.nombreUsuario(),
                rol,
                usuario.institucionId(),
                true);
    }
}
