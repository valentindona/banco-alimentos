package com.group6.BancoAlimentos.factura.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.mapper.FacturaMapper;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultaFacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private FacturaMapper facturaMapper;

    @Test
    void filtraPorInstitucionFechaEstadoYPagina() {
        ConsultaFacturaService servicio = new ConsultaFacturaService(facturaRepository, facturaMapper);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        Factura factura = Factura.builder()
                .id(10L)
                .numero("F-10")
                .institucionId(9L)
                .fecha(LocalDate.of(2026, 5, 1))
                .estado("PENDIENTE")
                .importe(new BigDecimal("1250.00"))
                .build();
        when(facturaRepository.buscarConFiltros(
                        9L, desde, hasta, "PENDIENTE", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(factura), PageRequest.of(0, 10), 1));
        when(facturaMapper.aRespuesta(factura)).thenReturn(
                new com.group6.BancoAlimentos.factura.dto.FacturaRespuesta(
                        10L, "F-10", 9L, null, null, factura.getFecha(), "PENDIENTE", factura.getImporte()));

        var pagina = servicio.listar(9L, desde, hasta, "PENDIENTE", 0, 10);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("F-10", pagina.getContent().get(0).numero());
        verify(facturaRepository).buscarConFiltros(
                9L, desde, hasta, "PENDIENTE", PageRequest.of(0, 10));
    }

    @Test
    void laProyeccionSeIncluyeEnLaAuditoriaDeNegocio() {
        assertNotNull(Factura.class.getAnnotation(Audited.class));
    }
}
