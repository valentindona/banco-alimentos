package com.group6.BancoAlimentos.itemdonacion.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemDonacionRespuesta(
        Long id,
        Long productoId,
        String nombreProducto,
        String tipoAlimento,
        String unidadMedida,
        BigDecimal cantidad,
        LocalDate fechaVencimiento) {}
