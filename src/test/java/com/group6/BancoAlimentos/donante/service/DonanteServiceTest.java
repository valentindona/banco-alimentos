package com.group6.BancoAlimentos.donante.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.donante.dto.DonanteSolicitud;
import com.group6.BancoAlimentos.donante.mapper.DonanteMapper;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.donante.repository.DonanteRepository;
import java.util.Optional;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonanteServiceTest {

    @Mock
    private DonanteRepository donanteRepository;

    @Test
    void creaDonanteConNombreObligatorioYContactoOpcional() {
        when(donanteRepository.save(any(Donante.class))).thenAnswer(invocacion -> {
            Donante donante = invocacion.getArgument(0);
            donante.setId(15L);
            return donante;
        });
        DonanteService servicio = new DonanteService(donanteRepository, new DonanteMapper());

        var respuesta = servicio.crear(new DonanteSolicitud(" Donante local ", null, null));

        assertEquals(15L, respuesta.id());
        assertEquals("Donante local", respuesta.nombre());
        assertNull(respuesta.correo());
        assertNull(respuesta.telefono());
        assertTrue(respuesta.activo());
    }

    @Test
    void rechazaNombreVacio() {
        DonanteService servicio = new DonanteService(donanteRepository, new DonanteMapper());

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(new DonanteSolicitud("  ", null, null)));
        verify(donanteRepository, never()).save(any(Donante.class));
    }

    @Test
    void actualizaDatosYNoPermiteReactivarUnDonanteDadoDeBaja() {
        Donante existente = Donante.builder()
                .id(20L)
                .nombre("Nombre anterior")
                .activo(true)
                .build();
        when(donanteRepository.findById(20L)).thenReturn(Optional.of(existente));
        when(donanteRepository.save(any(Donante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        DonanteService servicio = new DonanteService(donanteRepository, new DonanteMapper());

        var respuesta = servicio.actualizar(
                20L, new DonanteSolicitud("Nombre nuevo", "contacto@example.org", "123"));

        assertEquals("Nombre nuevo", respuesta.nombre());
        assertEquals("contacto@example.org", respuesta.correo());
        assertEquals("123", respuesta.telefono());

        existente.setActivo(false);
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.actualizar(20L, new DonanteSolicitud("Reactivado", null, null)));
    }

    @Test
    void rechazaUsarUnDonanteInactivo() {
        when(donanteRepository.findByIdAndActivoTrue(25L)).thenReturn(Optional.empty());
        DonanteService servicio = new DonanteService(donanteRepository, new DonanteMapper());

        assertThrows(ReglaNegocioException.class, () -> servicio.buscarActivo(25L));
    }

    @Test
    void daDeBajaLogicamenteYConservaLaAuditoria() {
        Donante existente = Donante.builder()
                .id(30L)
                .nombre("Donante con historial")
                .correo("donante@example.org")
                .activo(true)
                .build();
        when(donanteRepository.findById(30L)).thenReturn(Optional.of(existente));
        when(donanteRepository.save(any(Donante.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        DonanteService servicio = new DonanteService(donanteRepository, new DonanteMapper());

        servicio.darDeBaja(30L);

        assertFalse(existente.isActivo());
        assertEquals("Donante con historial", existente.getNombre());
        assertEquals("donante@example.org", existente.getCorreo());
        verify(donanteRepository).save(existente);
        verify(donanteRepository, never()).delete(any(Donante.class));
        assertNotNull(Donante.class.getAnnotation(Audited.class));
    }
}
