package com.group6.BancoAlimentos.producto.dto;

public record ProductoRespuesta(
        Long id,
        String nombre,
        String descripcion,
        String unidadMedida,
        boolean activo) {
}
