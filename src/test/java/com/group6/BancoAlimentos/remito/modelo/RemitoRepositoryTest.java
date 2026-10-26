package com.group6.BancoAlimentos.remito.modelo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RemitoRepositoryTest {

    @Autowired
    private RemitoRepository remitoRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void persisteBorradorConDatosObligatoriosYConservaInstantaneas() {
        Institucion institucion = crearInstitucion();
        Usuario usuario = crearUsuario();
        Remito remito = remitoRepository.saveAndFlush(Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio(institucion.getDomicilio())
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(usuario.getId())
                .build());

        assertTrue(remito.getId() > 0);
        assertEquals("Institución de prueba", remito.getInstitucionNombre());
        assertEquals("Calle de prueba 123", remito.getInstitucionDomicilio());
        assertNull(remito.getFechaConfirmacion());
        assertThrows(RuntimeException.class, () -> remitoRepository.saveAndFlush(Remito.builder()
                .institucionNombre("Sin destino")
                .institucionDomicilio("Calle")
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(usuario.getId())
                .build()));
    }

    private Institucion crearInstitucion() {
        return institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución de prueba")
                .domicilio("Calle de prueba 123")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
    }

    private Usuario crearUsuario() {
        return usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("deposito-prueba-" + java.util.UUID.randomUUID())
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
    }
}
