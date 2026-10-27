package com.group6.BancoAlimentos.institucion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.mapper.InstitucionMapper;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ConsultaInstitucionServiceTest {

    @Mock
    private InstitucionRepository institucionRepository;

    @Test
    void filtraPorNombreYEstadoPagoYPagina() {
        ConsultaInstitucionService servicio =
                new ConsultaInstitucionService(institucionRepository, new InstitucionMapper());
        Institucion institucion = Institucion.builder()
                .id(9L)
                .nombre("Comedor Norte")
                .domicilio("Calle 9")
                .estadoPago(EstadoPago.EN_DEUDA)
                .activa(true)
                .build();
        PageRequest solicitudPagina = PageRequest.of(0, 10, Sort.by("id").ascending());
        when(institucionRepository.buscarConFiltros(
                        "comedor", null, EstadoPago.EN_DEUDA, solicitudPagina))
                .thenReturn(new PageImpl<>(List.of(institucion), solicitudPagina, 1));

        var pagina = servicio.listar("comedor", null, EstadoPago.EN_DEUDA, 0, 10);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(EstadoPago.EN_DEUDA, pagina.getContent().get(0).estadoPago());
        verify(institucionRepository).buscarConFiltros(
                "comedor", null, EstadoPago.EN_DEUDA, solicitudPagina);
    }

    @Test
    void normalizaFiltrosYAplicaOrdenAscendentePorId() {
        ConsultaInstitucionService servicio =
                new ConsultaInstitucionService(institucionRepository, new InstitucionMapper());
        Institucion institucion = Institucion.builder()
                .id(2L)
                .nombre("Comedor Sur")
                .domicilio("Calle 2")
                .tipo("Comedor")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build();
        PageRequest paginaEsperada = PageRequest.of(0, 20, Sort.by("id").ascending());
        when(institucionRepository.buscarConFiltros(
                        "comedor", "Comedor", EstadoPago.AL_DIA, paginaEsperada))
                .thenReturn(new PageImpl<>(List.of(institucion), paginaEsperada, 1));

        var pagina = servicio.listar("  comedor  ", "  Comedor  ", EstadoPago.AL_DIA, 0, 20);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(2L, pagina.getContent().get(0).id());
        verify(institucionRepository).buscarConFiltros(
                "comedor", "Comedor", EstadoPago.AL_DIA, paginaEsperada);
    }

    @Test
    void normalizaPaginacionYLimitaElTamanoMaximo() {
        ConsultaInstitucionService servicio =
                new ConsultaInstitucionService(institucionRepository, new InstitucionMapper());
        PageRequest paginaEsperada = PageRequest.of(0, Paginacion.TAMANO_MAXIMO, Sort.by("id").ascending());
        when(institucionRepository.buscarConFiltros(null, null, null, paginaEsperada))
                .thenReturn(new PageImpl<>(List.of(), paginaEsperada, 0));

        var pagina = servicio.listar(" ", "  ", null, -1, 150);

        assertEquals(0, pagina.getTotalElements());
        assertEquals(Paginacion.TAMANO_MAXIMO, pagina.getSize());
        verify(institucionRepository).buscarConFiltros(null, null, null, paginaEsperada);
    }
}
