package com.group6.BancoAlimentos.Common.excepcion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ReglaNegocioExceptionTest {

    @Test
    void representaUnaReglaDeNegocioComoErrorNoProcesable() {
        ReglaNegocioException excepcion = new ReglaNegocioException("STOCK_VENCIDO", "La existencia está vencida.");

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, excepcion.getEstadoHttp());
        assertEquals("STOCK_VENCIDO", excepcion.getCodigo());
        assertEquals("La existencia está vencida.", excepcion.getMessage());
    }
}
