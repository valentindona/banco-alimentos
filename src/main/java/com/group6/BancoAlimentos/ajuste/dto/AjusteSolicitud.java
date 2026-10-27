package com.group6.BancoAlimentos.ajuste.dto;

import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record AjusteSolicitud(
        @NotNull @Positive Long stockId,
        @NotNull TipoAjuste tipo,
        @NotNull @DecimalMin("0.0") @Digits(integer = 9, fraction = 3) BigDecimal cantidad,
        @NotBlank String motivo) {
}
