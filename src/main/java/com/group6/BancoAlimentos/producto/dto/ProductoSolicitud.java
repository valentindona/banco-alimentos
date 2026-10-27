package com.group6.BancoAlimentos.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProductoSolicitud(
        @NotBlank @Size(max = 120) String nombre,
        @Size(max = 1000) String descripcion,
        @NotBlank @Size(max = 80) String tipoAlimento,
        @NotBlank @Size(max = 50) String unidadMedida) {}
