package com.group6.BancoAlimentos.Seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class ContratoDonacionesOpenApiTest {

    private static final Set<String> METODOS_HTTP =
            Set.of("get", "post", "put", "delete", "patch", "options", "head");

    @Test
    void contratoDeFeatureEstaIntegradoConRolesPaginacionYFacturasSoloLectura() throws Exception {
        String contrato = Files.readString(Path.of(
                "specs", "002-registro-donaciones", "contracts", "openapi.yaml"));
        String integrado = Files.readString(Path.of("src", "main", "resources", "openapi.yaml"));
        Matcher rutas = Pattern.compile("(?m)^  (/[^:\\r\\n]+):\\s*$").matcher(contrato);

        while (rutas.find()) {
            String ruta = rutas.group(1);
            String bloqueContrato = bloqueRuta(contrato, ruta);
            String bloqueIntegrado = bloqueRuta(integrado, ruta);
            assertNotNull(bloqueIntegrado, "Falta integrar la ruta " + ruta);

            Matcher metodos = Pattern.compile("(?m)^    ([a-z]+):\\s*$").matcher(bloqueContrato);
            while (metodos.find()) {
                String metodo = metodos.group(1);
                if (!METODOS_HTTP.contains(metodo)) {
                    continue;
                }
                String operacionContrato = bloqueOperacion(bloqueContrato, metodo);
                String operacionIntegrada = bloqueOperacion(bloqueIntegrado, metodo);
                assertNotNull(operacionIntegrada, "Falta " + metodo.toUpperCase() + " " + ruta);
                assertTrue(operacionContrato.contains("x-roles: [user_deposito]"));
                assertTrue(operacionIntegrada.contains("x-roles: [user_deposito]"),
                        "Falta el rol de depósito en " + metodo.toUpperCase() + " " + ruta);
            }
        }

        String facturas = bloqueRuta(integrado, "/facturas");
        assertNotNull(facturas);
        assertFalse(Pattern.compile("(?m)^    (post|put|patch|delete):").matcher(facturas).find(),
                "La API no debe exponer escritura de facturas");
        for (String ruta : Set.of("/donantes", "/donaciones", "/productos", "/stock", "/facturas")) {
            String operacion = bloqueOperacion(bloqueRuta(integrado, ruta), "get");
            assertNotNull(operacion);
            assertTrue(operacion.contains("#/components/parameters/Pagina"), "Falta paginación en " + ruta);
            assertTrue(operacion.contains("#/components/parameters/Tamano"), "Falta tamaño de página en " + ruta);
        }
        assertTrue(contrato.contains("detalles:"));
    }

    private String bloqueRuta(String yaml, String ruta) {
        Matcher matcher = Pattern.compile(
                        "(?ms)^  " + Pattern.quote(ruta) + ":\\s*$(.*?)(?=^  /|\\z)")
                .matcher(yaml);
        return matcher.find() ? matcher.group() : null;
    }

    private String bloqueOperacion(String bloque, String metodo) {
        Matcher matcher = Pattern.compile(
                        "(?ms)^    " + Pattern.quote(metodo) + ":\\s*$(.*?)(?=^    [a-z]+:|\\z)")
                .matcher(bloque);
        return matcher.find() ? matcher.group() : null;
    }
}
