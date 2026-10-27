package com.group6.BancoAlimentos.institucion.modelo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class InstitucionRepositoryTest {

    @Autowired
    private InstitucionRepository institucionRepository;

    @Test
    void requiereNombreDomicilioTipoYEstadoDePagoPermitido() {
        for (EstadoPago estadoPago : EstadoPago.values()) {
            Institucion institucion = Institucion.builder()
                    .nombre("Institución " + estadoPago)
                    .domicilio("Calle de prueba 123")
                    .estadoPago(estadoPago)
                    .activa(true)
                    .build();
            assertTrue(institucionRepository.saveAndFlush(institucion).getId() > 0);
        }

        Institucion institucionSinTipo = Institucion.builder()
                .nombre("Sin tipo")
                .domicilio("Calle de prueba")
                .estadoPago(EstadoPago.AL_DIA)
                .build();
        assertTrue(institucionRepository.saveAndFlush(institucionSinTipo).getId() > 0);
    }

    @Test
    void combinaFiltrosExcluyeInactivasYOrdenaPorId() {
        Institucion primera = guardar("Comedor Norte", "Comedor", EstadoPago.EN_DEUDA, true);
        guardar("Comedor Sur", "Comedor", EstadoPago.AL_DIA, true);
        guardar("Centro Norte", "Centro", EstadoPago.EN_DEUDA, true);
        guardar("Comedor Norte inactivo", "Comedor", EstadoPago.EN_DEUDA, false);

        Page<Institucion> pagina = institucionRepository.buscarConFiltros(
                "COMEDOR", "Comedor", EstadoPago.EN_DEUDA,
                PageRequest.of(0, 20, Sort.by("id").ascending()));

        assertEquals(List.of(primera.getId()), pagina.getContent().stream().map(Institucion::getId).toList());
        assertEquals(1, pagina.getTotalElements());
    }

    @Test
    void aplicaCoincidenciaParcialDeNombreYTipoExacto() {
        Institucion coincide = guardar("Comedor del Norte", "Comedor", EstadoPago.PARTICULAR, true);
        guardar("Comedor del Sur", "Centro Comedor", EstadoPago.PARTICULAR, true);

        Page<Institucion> pagina = institucionRepository.buscarConFiltros(
                "norte", "Comedor", EstadoPago.PARTICULAR,
                PageRequest.of(0, 20, Sort.by("id").ascending()));

        assertEquals(List.of(coincide.getId()), pagina.getContent().stream().map(Institucion::getId).toList());
        assertEquals(1, pagina.getTotalElements());
    }

    @Test
    void mantienePaginaVaciaYRespetaElTamanoMaximoComun() {
        Page<Institucion> paginaVacia = institucionRepository.buscarConFiltros(
                "sin coincidencias", null, null,
                PageRequest.of(0, 20, Sort.by("id").ascending()));
        Page<Institucion> paginaLimitada = institucionRepository.buscarConFiltros(
                null, null, null,
                Paginacion.crear(-1, Paginacion.TAMANO_MAXIMO + 50).withSort(Sort.by("id").ascending()));

        assertTrue(paginaVacia.isEmpty());
        assertEquals(0, paginaVacia.getTotalElements());
        assertEquals(Paginacion.TAMANO_MAXIMO, paginaLimitada.getSize());
        assertEquals(0, paginaLimitada.getNumber());
    }

    private Institucion guardar(String nombre, String tipo, EstadoPago estadoPago, boolean activa) {
        return institucionRepository.saveAndFlush(Institucion.builder()
                .nombre(nombre)
                .domicilio("Calle de prueba 123")
                .tipo(tipo)
                .estadoPago(estadoPago)
                .activa(activa)
                .build());
    }
}
