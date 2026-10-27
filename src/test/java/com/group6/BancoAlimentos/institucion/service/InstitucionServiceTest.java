package com.group6.BancoAlimentos.institucion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.institucion.dto.InstitucionSolicitud;
import com.group6.BancoAlimentos.institucion.mapper.InstitucionMapper;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitucionServiceTest {

    @Mock
    private InstitucionRepository institucionRepository;

    @Test
    void creaInstitucionNormalizandoDatosEInicializandoEstadoAlDia() {
        when(institucionRepository.existsByNombreIgnoreCase("Comedor Norte")).thenReturn(false);
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(invocacion -> {
            Institucion institucion = invocacion.getArgument(0);
            institucion.setId(15L);
            return institucion;
        });
        InstitucionService servicio = new InstitucionService(institucionRepository, new InstitucionMapper());

        var respuesta = servicio.crear(new InstitucionSolicitud(" Comedor Norte ", " Calle 1 ", " Comedor "));

        assertEquals(15L, respuesta.id());
        assertEquals("Comedor Norte", respuesta.nombre());
        assertEquals("Calle 1", respuesta.domicilio());
        assertEquals("Comedor", respuesta.tipo());
        assertEquals(EstadoPago.AL_DIA, respuesta.estadoPago());
        assertTrue(respuesta.activa());
    }

    @Test
    void rechazaNombresDuplicadosSinDistinguirMayusculas() {
        when(institucionRepository.existsByNombreIgnoreCase("comedor norte")).thenReturn(true);
        InstitucionService servicio = new InstitucionService(institucionRepository, new InstitucionMapper());

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(new InstitucionSolicitud("comedor norte", "Calle 1", "Comedor")));
        verify(institucionRepository, never()).save(any(Institucion.class));
    }

    @Test
    void actualizaDatosYPermiteConservarElNombreDeLaMismaInstitucion() {
        Institucion existente = Institucion.builder()
                .id(20L)
                .nombre("Comedor Norte")
                .domicilio("Calle vieja")
                .tipo("Comedor")
                .estadoPago(EstadoPago.EN_DEUDA)
                .activa(true)
                .build();
        when(institucionRepository.findByIdAndActivaTrue(20L)).thenReturn(Optional.of(existente));
        when(institucionRepository.existsByNombreIgnoreCaseAndIdNot("Comedor Norte", 20L)).thenReturn(false);
        when(institucionRepository.save(any(Institucion.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        InstitucionService servicio = new InstitucionService(institucionRepository, new InstitucionMapper());

        var respuesta = servicio.actualizar(
                20L, new InstitucionSolicitud("Comedor Norte", "Calle nueva", "Centro comunitario"));

        assertEquals("Calle nueva", respuesta.domicilio());
        assertEquals("Centro comunitario", respuesta.tipo());
        assertEquals(EstadoPago.EN_DEUDA, respuesta.estadoPago());
        assertTrue(existente.isActiva());
    }

    @Test
    void rechazaActualizarUnaInstitucionInexistenteODadaDeBaja() {
        when(institucionRepository.findByIdAndActivaTrue(25L)).thenReturn(Optional.empty());
        InstitucionService servicio = new InstitucionService(institucionRepository, new InstitucionMapper());
        InstitucionSolicitud solicitud = new InstitucionSolicitud("Comedor", "Calle 1", "Comedor");

        assertThrows(ReglaNegocioException.class, () -> servicio.actualizar(25L, solicitud));
        verify(institucionRepository, never()).save(any(Institucion.class));
    }

    @Test
    void rechazaCambiarElNombrePorElDeOtraInstitucion() {
        Institucion existente = Institucion.builder()
                .id(20L)
                .nombre("Comedor Norte")
                .domicilio("Calle 1")
                .tipo("Comedor")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build();
        when(institucionRepository.findByIdAndActivaTrue(20L)).thenReturn(Optional.of(existente));
        when(institucionRepository.existsByNombreIgnoreCaseAndIdNot("Comedor Sur", 20L)).thenReturn(true);
        InstitucionService servicio = new InstitucionService(institucionRepository, new InstitucionMapper());

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.actualizar(20L, new InstitucionSolicitud("Comedor Sur", "Calle 1", "Comedor")));
        verify(institucionRepository, never()).save(any(Institucion.class));
    }

    @Test
    void daDeBajaLogicamenteYRepetirLaBajaNoFalla() {
        Institucion existente = Institucion.builder()
                .id(30L)
                .nombre("Comedor con historial")
                .domicilio("Calle 1")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build();
        when(institucionRepository.findById(30L)).thenReturn(Optional.of(existente));
        InstitucionService servicio = new InstitucionService(institucionRepository, new InstitucionMapper());

        servicio.darDeBaja(30L);
        assertFalse(existente.isActiva());
        servicio.darDeBaja(30L);

        verify(institucionRepository).save(existente);
        verify(institucionRepository, never()).delete(any(Institucion.class));
    }

    @Test
    void rechazaDarDeBajaUnaInstitucionInexistente() {
        when(institucionRepository.findById(404L)).thenReturn(Optional.empty());
        InstitucionService servicio = new InstitucionService(institucionRepository, new InstitucionMapper());

        assertThrows(ReglaNegocioException.class, () -> servicio.darDeBaja(404L));
        verify(institucionRepository, never()).save(any(Institucion.class));
    }
}
