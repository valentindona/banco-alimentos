package com.group6.BancoAlimentos.detalleremito.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DetalleRemitoRespuesta(
        Long id,
        Long productoId,
        String productoNombre,
        Long stockId,
        LocalDate fechaVencimiento,
        BigDecimal cantidad) {
}
