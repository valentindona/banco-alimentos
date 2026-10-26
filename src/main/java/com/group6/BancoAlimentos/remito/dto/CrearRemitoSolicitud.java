package com.group6.BancoAlimentos.remito.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record CrearRemitoSolicitud(
        @NotNull Long institucionId,
        @NotNull @Future LocalDate fechaEntrega) {
}
