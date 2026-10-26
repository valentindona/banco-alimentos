package com.group6.BancoAlimentos.notificacion.dto;

import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import java.time.Instant;

public record NotificacionRespuesta(
        Long id,
        Long stockId,
        TipoNotificacion tipo,
        String audiencia,
        Instant fechaCreacion,
        String mensaje) {
}
