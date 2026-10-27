package com.group6.BancoAlimentos.itemdonacion.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemDonacionSolicitud(
        Long productoId,
        @Size(max = 120) String nombreProducto,
        @Size(max = 80) String tipoAlimento,
        @Size(max = 50) String unidadMedida,
        @NotNull @DecimalMin(value = "0", inclusive = false) @DecimalMax("999999999.999")
                @Digits(integer = 9, fraction = 3)
                BigDecimal cantidad,
        @NotNull LocalDate fechaVencimiento) {}
