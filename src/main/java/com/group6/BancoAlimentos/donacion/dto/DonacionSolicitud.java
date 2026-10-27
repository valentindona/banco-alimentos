package com.group6.BancoAlimentos.donacion.dto;

import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionSolicitud;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record DonacionSolicitud(
        @NotNull Long donanteId,
        @NotNull LocalDate fecha,
        @NotEmpty List<@Valid ItemDonacionSolicitud> items) {}
