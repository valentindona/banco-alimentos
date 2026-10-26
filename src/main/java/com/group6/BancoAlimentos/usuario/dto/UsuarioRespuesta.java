package com.group6.BancoAlimentos.usuario.dto;

import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;

public record UsuarioRespuesta(
        Long id,
        String nombreUsuario,
        RolUsuario rol,
        Long institucionId,
        boolean activo) {
}
