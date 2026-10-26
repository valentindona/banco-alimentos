package com.group6.BancoAlimentos.Common.respuesta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorRespuestaTest {

    @Test
    void conservaElFormatoEstandarizadoDeError() {
        OffsetDateTime fechaHora = OffsetDateTime.parse("2026-10-26T12:30:00Z");
        ErrorRespuesta respuesta = new ErrorRespuesta(
                fechaHora,
                422,
                "REGLA_NEGOCIO",
                "La existencia está vencida.",
                "/api/remitos",
                List.of(new ErrorRespuesta.ErrorCampo("stockId", "No se puede entregar.")));

        assertEquals(fechaHora, respuesta.fechaHora());
        assertEquals(422, respuesta.estado());
        assertEquals("REGLA_NEGOCIO", respuesta.codigo());
        assertEquals("/api/remitos", respuesta.ruta());
        assertEquals(1, respuesta.erroresCampo().size());
        assertNotNull(respuesta.mensaje());
    }
}
