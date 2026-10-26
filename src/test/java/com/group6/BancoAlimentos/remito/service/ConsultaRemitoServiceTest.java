package com.group6.BancoAlimentos.remito.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultaRemitoServiceTest {

    @Mock
    private RemitoRepository remitoRepository;

    @Mock
    private DetalleRemitoRepository detalleRemitoRepository;

    @Test
    void aplicaFiltrosYPaginacionYDevuelvePaginaVaciaCuandoNoHayCoincidencias() {
        ConsultaRemitoService servicio =
                new ConsultaRemitoService(remitoRepository, detalleRemitoRepository);
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        when(remitoRepository.buscarConFiltros(
                        27L, desde, hasta, EstadoRemito.CONFIRMADO, PageRequest.of(1, 5)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(1, 5), 0));

        var pagina = servicio.listar(27L, desde, hasta, EstadoRemito.CONFIRMADO, 1, 5);

        assertEquals(0, pagina.getTotalElements());
        assertEquals(1, pagina.getNumber());
        assertEquals(5, pagina.getSize());
        verify(remitoRepository).buscarConFiltros(
                27L, desde, hasta, EstadoRemito.CONFIRMADO, PageRequest.of(1, 5));
    }

    @Test
    void aplicaFiltrosFechaInstitucionEstadoYPaginaConDatos() {
        ConsultaRemitoService servicio =
                new ConsultaRemitoService(remitoRepository, detalleRemitoRepository);
        LocalDate desde = LocalDate.of(2026, 10, 1);
        LocalDate hasta = LocalDate.of(2026, 10, 31);
        Remito remito = Remito.builder()
                .id(12L)
                .institucionId(27L)
                .institucionNombre("Comedor")
                .institucionDomicilio("Calle 1")
                .fechaEntrega(LocalDate.of(2026, 10, 15))
                .estado(EstadoRemito.CONFIRMADO)
                .build();
        when(remitoRepository.buscarConFiltros(
                        27L, desde, hasta, EstadoRemito.CONFIRMADO, PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(remito), PageRequest.of(0, 10), 1));
        when(detalleRemitoRepository.findByRemitoIdOrderByStockIdAsc(12L)).thenReturn(List.of());

        var pagina = servicio.listar(27L, desde, hasta, EstadoRemito.CONFIRMADO, 0, 10);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(12L, pagina.getContent().get(0).id());
        assertEquals(27L, pagina.getContent().get(0).institucionId());
        verify(remitoRepository).buscarConFiltros(
                27L, desde, hasta, EstadoRemito.CONFIRMADO, PageRequest.of(0, 10));
    }

    @Test
    void limitaTamanoDePaginaYNormalizaNumeroNegativo() {
        ConsultaRemitoService servicio =
                new ConsultaRemitoService(remitoRepository, detalleRemitoRepository);
        when(remitoRepository.buscarConFiltros(null, null, null, null, PageRequest.of(0, 100)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

        servicio.listar(null, null, null, null, -1, 500);

        verify(remitoRepository).buscarConFiltros(null, null, null, null, PageRequest.of(0, 100));
    }
}
