package com.group6.BancoAlimentos.Security;

import java.util.List;

public record UsuarioAutenticado(
        long usuarioId,
        String nombreUsuario,
        List<String> roles,
        Long institucionId) {
}
