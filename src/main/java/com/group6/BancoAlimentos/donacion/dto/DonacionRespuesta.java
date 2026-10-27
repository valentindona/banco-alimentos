package com.group6.BancoAlimentos.donacion.dto;

import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import java.time.LocalDate;
import java.util.List;

public record DonacionRespuesta(
        Long id,
        Long donanteId,
        String donanteNombre,
        LocalDate fecha,
        List<ItemDonacionRespuesta> items,
        ComprobanteRespuesta comprobante) {}
