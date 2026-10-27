package com.group6.BancoAlimentos.Seguridad;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ContratoInstitucionesOpenApiTest {

    @Test
    void integraEndpointsDeAbmConSusRolesYEsquemas() throws Exception {
        String contrato = Files.readString(Path.of(
                "specs", "003-gestion-instituciones-pagos", "contracts", "openapi.yaml"));
        String integrado = Files.readString(Path.of("src", "main", "resources", "openapi.yaml"));

        String[][] operaciones = {
            {"/instituciones", "post"},
            {"/instituciones/{institucionId}", "get"},
            {"/instituciones/{institucionId}", "put"},
            {"/instituciones/{institucionId}", "delete"}
        };
        for (String[] rutaMetodo : operaciones) {
            String ruta = rutaMetodo[0];
            String metodo = rutaMetodo[1];
            String operacionContrato = bloqueOperacion(bloqueRuta(contrato, ruta), metodo);
            String operacionIntegrada = bloqueOperacion(bloqueRuta(integrado, ruta), metodo);

            assertNotNull(operacionContrato, "El contrato no define " + metodo + " " + ruta);
            assertNotNull(operacionIntegrada, "Falta integrar " + metodo + " " + ruta);
            if ("get".equals(metodo)) {
                for (String rol : new String[] {"user_institucional", "user_tesoreria", "user_deposito"}) {
                    assertTrue(operacionIntegrada.contains(rol),
                            "Falta " + rol + " para " + metodo + " " + ruta);
                }
            } else {
                assertTrue(operacionIntegrada.contains("x-roles: [user_institucional]"),
                        "Falta el rol institucional para " + metodo + " " + ruta);
            }
        }

        assertTrue(bloqueOperacion(bloqueRuta(integrado, "/instituciones"), "post")
                .contains("#/components/schemas/InstitucionSolicitud"));
        assertTrue(bloqueOperacion(bloqueRuta(integrado, "/instituciones/{institucionId}"), "put")
                .contains("#/components/schemas/InstitucionSolicitud"));
        assertTrue(integrado.contains("InstitucionSolicitud:"));
        assertTrue(integrado.contains("Institucion:"));
        assertTrue(integrado.contains("tipo:"));
    }

    @Test
    void integraCambioDeEstadoPagoSoloParaTesoreria() throws Exception {
        String contrato = Files.readString(Path.of(
                "specs", "003-gestion-instituciones-pagos", "contracts", "openapi.yaml"));
        String integrado = Files.readString(Path.of("src", "main", "resources", "openapi.yaml"));
        String ruta = "/instituciones/{institucionId}/estado-pago";

        String operacionContrato = bloqueOperacion(bloqueRuta(contrato, ruta), "patch");
        String operacionIntegrada = bloqueOperacion(bloqueRuta(integrado, ruta), "patch");

        assertNotNull(operacionContrato, "El contrato no define PATCH " + ruta);
        assertNotNull(operacionIntegrada, "Falta integrar PATCH " + ruta);
        assertTrue(operacionContrato.contains("x-roles: [user_tesoreria]"));
        assertTrue(operacionIntegrada.contains("x-roles: [user_tesoreria]"));
        assertTrue(operacionIntegrada.contains("#/components/schemas/EstadoPagoSolicitud"));
        assertTrue(integrado.contains("EstadoPagoSolicitud:"));
        assertTrue(integrado.contains("enum: [AL_DIA, EN_DEUDA, PARTICULAR]"));
    }

    private String bloqueRuta(String yaml, String ruta) {
        Matcher matcher = Pattern.compile(
                        "(?ms)^  " + Pattern.quote(ruta) + ":\\s*$(.*?)(?=^  /|\\z)")
                .matcher(yaml);
        return matcher.find() ? matcher.group() : null;
    }

    private String bloqueOperacion(String bloque, String metodo) {
        if (bloque == null) {
            return null;
        }
        Matcher matcher = Pattern.compile(
                        "(?ms)^    " + Pattern.quote(metodo) + ":\\s*$(.*?)(?=^    [a-z]+:|\\z)")
                .matcher(bloque);
        return matcher.find() ? matcher.group() : null;
    }
}
