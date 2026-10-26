package com.group6.BancoAlimentos.Security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.group6.BancoAlimentos.Config.PropiedadesJwt;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import io.jsonwebtoken.JwtException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class TokenJwtServiceTest {

    private static final String CLAVE_PRUEBA = "clave-de-prueba-solo-para-firmar-tokens-jwt";
    private static final Instant AHORA = Instant.parse("2026-10-26T12:00:00Z");

    @Test
    void generaYValidaFirmaExpiracionYRoles() {
        TokenJwtService servicio = servicio(Clock.fixed(AHORA, ZoneOffset.UTC));
        String token = servicio.generarToken(usuario());

        IdentidadJwt identidad = servicio.validarToken(token);

        assertEquals(42L, identidad.usuarioId());
        assertEquals("deposito", identidad.nombreUsuario());
        assertEquals("user_deposito", identidad.roles().getFirst());
    }

    @Test
    void rechazaFirmasIncorrectasTokensVencidosYTokensMalformados() {
        String token = servicio(Clock.fixed(AHORA, ZoneOffset.UTC)).generarToken(usuario());
        TokenJwtService otroFirmante = servicio(Clock.fixed(AHORA, ZoneOffset.UTC), "otra-clave-de-prueba-suficientemente-larga");
        TokenJwtService vencido = servicio(Clock.fixed(AHORA.plus(Duration.ofHours(2)), ZoneOffset.UTC));

        assertThrows(JwtException.class, () -> otroFirmante.validarToken(token));
        assertThrows(JwtException.class, () -> vencido.validarToken(token));
        assertThrows(JwtException.class, () -> vencido.validarToken("no-es-un-jwt"));
    }

    private TokenJwtService servicio(Clock reloj) {
        return servicio(reloj, CLAVE_PRUEBA);
    }

    private TokenJwtService servicio(Clock reloj, String clave) {
        PropiedadesJwt propiedades = new PropiedadesJwt();
        propiedades.setSecreto(clave);
        propiedades.setDuracion(Duration.ofHours(1));
        return new TokenJwtService(propiedades, reloj);
    }

    private Usuario usuario() {
        return Usuario.builder()
                .id(42L)
                .nombreUsuario("deposito")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build();
    }
}
