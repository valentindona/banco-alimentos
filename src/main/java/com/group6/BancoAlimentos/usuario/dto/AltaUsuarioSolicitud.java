package com.group6.BancoAlimentos.usuario.dto;

import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AltaUsuarioSolicitud(
        @NotBlank @Size(max = 100) String nombreUsuario,
        @NotBlank @Size(min = 12, max = 128) String clave,
        @NotNull RolUsuario rol,
        Long institucionId) {

    @AssertTrue(message = "El rol institucional requiere una institución; los demás roles no la admiten.")
    public boolean isInstitucionConsistente() {
        if (rol == null) {
            return true;
        }
        return rol == RolUsuario.USER_INSTITUCIONAL ? institucionId != null : institucionId == null;
    }
}
