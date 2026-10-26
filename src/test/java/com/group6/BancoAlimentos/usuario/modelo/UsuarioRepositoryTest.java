package com.group6.BancoAlimentos.usuario.modelo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Test
    void persisteRolesDeDepositoYTesoreriaSinInstitucion() {
        Usuario deposito = crearUsuario("deposito", RolUsuario.USER_DEPOSITO, null);
        Usuario tesoreria = crearUsuario("tesoreria", RolUsuario.USER_TESORERIA, null);

        assertTrue(usuarioRepository.saveAndFlush(deposito).getId() > 0);
        assertTrue(usuarioRepository.saveAndFlush(tesoreria).getId() > 0);
    }

    @Test
    void requiereInstitucionParaUsuarioInstitucional() {
        Usuario institucionalSinInstitucion = crearUsuario("institucional", RolUsuario.USER_INSTITUCIONAL, null);
        assertThrows(RuntimeException.class, () -> usuarioRepository.saveAndFlush(institucionalSinInstitucion));
    }

    @Test
    void persisteUsuarioInstitucionalAsociadoAInstitucion() {
        var institucion = institucionRepository.saveAndFlush(
                com.group6.BancoAlimentos.institucion.modelo.Institucion.builder()
                        .nombre("Institución de prueba")
                        .domicilio("Calle de prueba 123")
                        .estadoPago(com.group6.BancoAlimentos.institucion.modelo.EstadoPago.AL_DIA)
                        .activa(true)
                        .build());
        Usuario institucional = crearUsuario("institucional-valido", RolUsuario.USER_INSTITUCIONAL, institucion.getId());

        assertTrue(usuarioRepository.saveAndFlush(institucional).getId() > 0);
    }

    @Test
    void exigeNombreDeUsuarioYClaveHash() {
        assertThrows(RuntimeException.class, () -> usuarioRepository.saveAndFlush(new Usuario()));
    }

    @Test
    void exigeUnicidadDelNombreDeUsuario() {
        usuarioRepository.saveAndFlush(crearUsuario("unico", RolUsuario.USER_DEPOSITO, null));
        assertThrows(DataIntegrityViolationException.class,
                () -> usuarioRepository.saveAndFlush(crearUsuario("unico", RolUsuario.USER_DEPOSITO, null)));
    }

    private Usuario crearUsuario(String nombreUsuario, RolUsuario rol, Long institucionId) {
        return Usuario.builder()
                .nombreUsuario(nombreUsuario)
                .claveHash("$2a$10$hash-de-prueba")
                .rol(rol)
                .institucionId(institucionId)
                .activo(true)
                .build();
    }
}
