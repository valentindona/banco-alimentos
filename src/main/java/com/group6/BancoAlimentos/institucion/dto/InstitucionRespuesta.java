package com.group6.BancoAlimentos.institucion.dto;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;

public record InstitucionRespuesta(
        Long id,
        String nombre,
        String domicilio,
        String tipo,
        EstadoPago estadoPago,
        boolean activa) {
}
