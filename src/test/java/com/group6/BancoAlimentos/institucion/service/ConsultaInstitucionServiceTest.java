package com.group6.BancoAlimentos.institucion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
        when(institucionRepository.buscarConFiltros("comedor", EstadoPago.EN_DEUDA,
                        PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(institucion), PageRequest.of(0, 10), 1));

        var pagina = servicio.listar("comedor", EstadoPago.EN_DEUDA, 0, 10);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(EstadoPago.EN_DEUDA, pagina.getContent().get(0).estadoPago());
        verify(institucionRepository).buscarConFiltros(
                "comedor", EstadoPago.EN_DEUDA, PageRequest.of(0, 10));
    }
}
