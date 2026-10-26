package com.group6.BancoAlimentos.usuario.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.institucion.service.ConsultaInstitucionService;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class ProvisionamientoUsuariosServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private ConsultaInstitucionService consultaInstitucionService;

    @InjectMocks
    private ProvisionamientoUsuariosService provisionamientoUsuariosService;

    @Test
    void creaSoloUnUsuarioDeDepositoConClaveCodificada() {
        when(usuarioRepository.existsByNombreUsuario("deposito-inicial")).thenReturn(false);
        when(passwordEncoder.encode("clave-inicial-segura")).thenReturn("hash-adaptativo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ArgumentCaptor<Usuario> capturador = ArgumentCaptor.forClass(Usuario.class);

        provisionamientoUsuariosService.crearUsuarioDepositoInicial("deposito-inicial", "clave-inicial-segura");

        verify(usuarioRepository).save(capturador.capture());
        assertEquals(RolUsuario.USER_DEPOSITO, capturador.getValue().getRol());
        assertEquals("hash-adaptativo", capturador.getValue().getClaveHash());
        verify(passwordEncoder).encode("clave-inicial-segura");
    }

    @Test
    void noReemplazaUnaCuentaInicialYaProvisionada() {
        when(usuarioRepository.existsByNombreUsuario("deposito-inicial")).thenReturn(true);

        provisionamientoUsuariosService.crearUsuarioDepositoInicial("deposito-inicial", "clave-inicial-segura");

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(passwordEncoder, never()).encode(any());
    }
}
