package com.group6.BancoAlimentos.factura.dto;

import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FacturaRespuesta(
        Long id,
        String numero,
        Long institucionId,
        Long donanteId,
        Long donacionId,
        LocalDate fecha,
        String estado,
        BigDecimal importe,
        List<ItemDonacionRespuesta> detalles) {
}
