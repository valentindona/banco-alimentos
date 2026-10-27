package com.group6.BancoAlimentos.producto.dto;

public record ProductoRespuesta(
        Long id,
        String nombre,
        String descripcion,
        String tipoAlimento,
        String unidadMedida,
        boolean activo) {
}
