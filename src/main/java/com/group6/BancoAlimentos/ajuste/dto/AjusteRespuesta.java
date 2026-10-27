package com.group6.BancoAlimentos.ajuste.dto;

import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record AjusteRespuesta(
        Long id,
        Long stockId,
        Long productoId,
        LocalDate fechaVencimiento,
        Long usuarioId,
        Instant fechaHora,
        TipoAjuste tipo,
        BigDecimal cantidadAnterior,
        BigDecimal cantidadNueva,
        String motivo) {
}
