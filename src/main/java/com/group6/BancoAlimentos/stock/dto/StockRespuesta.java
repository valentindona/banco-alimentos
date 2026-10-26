package com.group6.BancoAlimentos.stock.dto;

import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import java.math.BigDecimal;
import java.time.LocalDate;

public record StockRespuesta(
        Long id,
        Long productoId,
        String productoNombre,
        LocalDate fechaVencimiento,
        BigDecimal cantidadDisponible,
        EstadoStock estado) {
}
