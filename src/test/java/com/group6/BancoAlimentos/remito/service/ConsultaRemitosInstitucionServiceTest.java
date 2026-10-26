package com.group6.BancoAlimentos.remito.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultaRemitosInstitucionServiceTest {

    @Mock
    private RemitoRepository remitoRepository;

    @Mock
    private DetalleRemitoRepository detalleRemitoRepository;

    @Test
    void limitaConsultaAlInstitutoDelTokenIgnorandoInstitucionSolicitada() {
        ConsultaRemitoService servicio =
                new ConsultaRemitoService(remitoRepository, detalleRemitoRepository);
        UsuarioAutenticado usuario =
                new UsuarioAutenticado(8L, "institucional", List.of("user_institucional"), 27L);
        when(remitoRepository.buscarConFiltros(27L, null, null, null, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var pagina = servicio.listar(99L, null, null, null, 0, 20, usuario);

        assertEquals(0, pagina.getTotalElements());
        verify(remitoRepository).buscarConFiltros(27L, null, null, null, PageRequest.of(0, 20));
    }
}
