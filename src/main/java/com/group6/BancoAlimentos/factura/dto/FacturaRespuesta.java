package com.group6.BancoAlimentos.factura.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record FacturaRespuesta(
        Long id,
        String numero,
        Long institucionId,
        LocalDate fecha,
        String estado,
        BigDecimal importe) {
}
