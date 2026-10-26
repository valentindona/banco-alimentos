package com.group6.BancoAlimentos.detalleremito.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record AgregarDetalleSolicitud(
        @NotNull Long stockId,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal cantidad) {
}
