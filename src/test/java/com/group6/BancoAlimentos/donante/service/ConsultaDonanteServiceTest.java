package com.group6.BancoAlimentos.donante.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.donante.mapper.DonanteMapper;
import com.group6.BancoAlimentos.donante.repository.DonanteRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultaDonanteServiceTest {

    @Mock
    private DonanteRepository donanteRepository;

    @Mock
    private DonanteMapper donanteMapper;

    @Test
    void filtraPorNombreYEstadoYPagina() {
        ConsultaDonanteService servicio = new ConsultaDonanteService(donanteRepository, donanteMapper);
        Donante donante = Donante.builder()
                .id(7L)
                .nombre("Comedor del barrio")
                .activo(false)
                .build();
        when(donanteRepository.buscarConFiltros("comedor", false, PageRequest.of(0, 3)))
                .thenReturn(new PageImpl<>(List.of(donante), PageRequest.of(0, 3), 1));
        when(donanteMapper.aRespuesta(donante)).thenReturn(
                new com.group6.BancoAlimentos.donante.dto.DonanteRespuesta(
                        7L, "Comedor del barrio", null, null, false));

        var resultado = servicio.listar(" comedor ", false, 0, 3);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Comedor del barrio", resultado.getContent().get(0).nombre());
        assertEquals(false, resultado.getContent().get(0).activo());
        verify(donanteRepository).buscarConFiltros("comedor", false, PageRequest.of(0, 3));
    }
}
