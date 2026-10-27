package com.group6.BancoAlimentos.donacion.dto;

import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import java.time.LocalDate;
import java.util.List;

public record ComprobanteRespuesta(
        Long id,
        String numero,
        Long donacionId,
        Long donanteId,
        LocalDate fecha,
        List<ItemDonacionRespuesta> detalles) {}
