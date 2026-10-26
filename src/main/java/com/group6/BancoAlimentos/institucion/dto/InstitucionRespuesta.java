package com.group6.BancoAlimentos.institucion.dto;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;

public record InstitucionRespuesta(
        Long id,
        String nombre,
        String domicilio,
        EstadoPago estadoPago,
        boolean activa) {
}
