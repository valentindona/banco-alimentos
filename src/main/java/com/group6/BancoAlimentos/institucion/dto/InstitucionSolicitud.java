package com.group6.BancoAlimentos.institucion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InstitucionSolicitud(
        @NotBlank @Size(max = 150) String nombre,
        @NotBlank @Size(max = 250) String domicilio,
        @NotBlank @Size(max = 80) String tipo) {
}
