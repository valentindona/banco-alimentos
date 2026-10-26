package com.group6.BancoAlimentos.usuario.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginSolicitud(
        @NotBlank String nombreUsuario,
        @NotBlank String clave) {
}
