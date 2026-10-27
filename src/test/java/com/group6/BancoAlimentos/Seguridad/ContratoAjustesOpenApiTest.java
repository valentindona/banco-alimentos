package com.group6.BancoAlimentos.Seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ContratoAjustesOpenApiTest {

    @Test
    void contratoDeAjustesEstaIntegradoConFiltrosPermisosYPaginacion() throws Exception {
        String contrato = Files.readString(Path.of(
                "specs", "004-ajustes-manuales-stock", "contracts", "ajustes-stock.openapi.yaml"));
        String integrado = Files.readString(Path.of("src", "main", "resources", "openapi.yaml"));
        String ruta = bloqueRuta(integrado, "/ajustes");
        assertNotNull(ruta);
        assertTrue(ruta.contains("post:"));
        assertTrue(ruta.contains("get:"));
        assertTrue(ruta.contains("x-roles: [user_deposito]"));
        assertTrue(ruta.contains("fechaVencimiento"));
        assertTrue(ruta.contains("fechaDesde"));
        assertTrue(ruta.contains("fechaHasta"));
        assertTrue(ruta.contains("usuarioId"));
        assertTrue(ruta.contains("tipo"));
        assertTrue(ruta.contains("#/components/parameters/Pagina"));
        assertTrue(ruta.contains("#/components/parameters/Tamano"));
        String operacionesEdicion = bloqueRuta(integrado, "/ajustes/{ajusteId}");
        assertNotNull(operacionesEdicion);
        assertTrue(operacionesEdicion.contains("put:"));
        assertTrue(operacionesEdicion.contains("delete:"));
        for (String nombre : new String[] {"AjusteSolicitud", "AjusteRespuesta", "PaginaAjusteRespuesta"}) {
            assertTrue(integrado.contains(nombre), "No se documentó " + nombre);
        }
        assertTrue(contrato.contains("operationId: registrarAjusteStock"));
        assertTrue(contrato.contains("operationId: listarAjustesStock"));
        assertTrue(contrato.contains("operationId: actualizarAjusteStock"));
        assertTrue(contrato.contains("operationId: eliminarAjusteStock"));
    }

    private String bloqueRuta(String yaml, String ruta) {
        var matcher = java.util.regex.Pattern.compile(
                        "(?ms)^  " + java.util.regex.Pattern.quote(ruta) + ":\\s*$(.*?)(?=^  /|\\z)")
                .matcher(yaml);
        return matcher.find() ? matcher.group() : null;
    }
}
