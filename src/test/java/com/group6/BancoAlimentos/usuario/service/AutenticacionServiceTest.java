package com.group6.BancoAlimentos.usuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Security.TokenJwtService;
import com.group6.BancoAlimentos.usuario.dto.LoginSolicitud;
import com.group6.BancoAlimentos.usuario.dto.TokenRespuesta;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AutenticacionServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenJwtService tokenJwtService;

    @InjectMocks
    private AutenticacionService autenticacionService;

    @Test
    void autenticaCredencialesValidasYEmiteUnToken() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByNombreUsuario("valido")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("clave-correcta", usuario.getClaveHash())).thenReturn(true);
        when(tokenJwtService.crearRespuesta(usuario)).thenReturn(new TokenRespuesta("jwt-firmado", "Bearer", 3600));

        TokenRespuesta respuesta = autenticacionService.iniciarSesion(
                new LoginSolicitud("valido", "clave-correcta"));

        assertEquals("jwt-firmado", respuesta.token());
        verify(tokenJwtService).crearRespuesta(usuario);
    }

    @Test
    void rechazaCredencialesInvalidas() {
        Usuario usuario = usuarioActivo();
        when(usuarioRepository.findByNombreUsuario("valido")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("incorrecta", usuario.getClaveHash())).thenReturn(false);

        assertThrows(BadCredentialsException.class,
                () -> autenticacionService.iniciarSesion(new LoginSolicitud("valido", "incorrecta")));
    }

    @Test
    void rechazaCuentasDeshabilitadasAntesDeVerificarLaClave() {
        Usuario usuario = usuarioActivo();
        usuario.setActivo(false);
        when(usuarioRepository.findByNombreUsuario("valido")).thenReturn(Optional.of(usuario));

        assertThrows(DisabledException.class,
                () -> autenticacionService.iniciarSesion(new LoginSolicitud("valido", "clave-correcta")));
    }

    @Test
    void codificaClavesAlCrearUsuarios() {
        when(passwordEncoder.encode("clave-nueva")).thenReturn("hash-adaptativo");

        String hash = autenticacionService.codificarClave("clave-nueva");

        assertEquals("hash-adaptativo", hash);
        verify(passwordEncoder).encode("clave-nueva");
    }

    private Usuario usuarioActivo() {
        return Usuario.builder()
                .id(10L)
                .nombreUsuario("valido")
                .claveHash("hash-guardado")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .creadoEn(Instant.parse("2026-10-26T12:00:00Z"))
                .build();
    }
}
