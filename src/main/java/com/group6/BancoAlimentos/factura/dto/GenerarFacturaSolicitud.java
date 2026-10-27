package com.group6.BancoAlimentos.factura.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record GenerarFacturaSolicitud(@NotNull @Positive Long donacionId) {}
