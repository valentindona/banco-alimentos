package com.group6.BancoAlimentos.institucion.dto;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import jakarta.validation.constraints.NotNull;

public record EstadoPagoSolicitud(@NotNull EstadoPago estadoPago) {
}
