package com.group6.BancoAlimentos.Common.respuesta;

import java.time.OffsetDateTime;
import java.util.List;

public record ErrorRespuesta(
        OffsetDateTime fechaHora,
        int estado,
        String codigo,
        String mensaje,
        String ruta,
        List<ErrorCampo> erroresCampo) {

    public ErrorRespuesta {
        erroresCampo = erroresCampo == null ? List.of() : List.copyOf(erroresCampo);
    }

    public record ErrorCampo(String campo, String mensaje) {
    }
}
