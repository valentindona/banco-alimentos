package com.group6.BancoAlimentos.factura.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.mapper.FacturaMapper;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.mapper.ItemDonacionMapper;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.producto.modelo.Producto;
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
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ConsultaFacturaServiceTest {

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private FacturaMapper facturaMapper;

    @Mock
    private ItemDonacionRepository itemDonacionRepository;

    @Mock
    private ItemDonacionMapper itemDonacionMapper;

    @Test
    void filtraPorDonanteProductoVencimientoFechasEstadoYPaginaYConservaDetalle() {
        ConsultaFacturaService servicio =
                new ConsultaFacturaService(
                        facturaRepository, facturaMapper, itemDonacionRepository, itemDonacionMapper);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        LocalDate vencimiento = LocalDate.of(2027, 2, 1);
        Donacion donacion = Donacion.builder().id(20L).donante(Donante.builder().id(9L).build())
                .fecha(LocalDate.of(2026, 5, 1)).build();
        Producto producto = Producto.builder().id(4L).nombre("Harina").build();
        ItemDonacion item = ItemDonacion.builder()
                .id(30L)
                .donacion(donacion)
                .producto(producto)
                .productoNombre("Harina integral")
                .tipoAlimento("Harina")
                .unidadMedida("kilos")
                .cantidad(new BigDecimal("2.500"))
                .fechaVencimiento(vencimiento)
                .build();
        Factura factura = Factura.builder()
                .id(10L)
                .numero("10")
                .donanteId(9L)
                .donacionId(20L)
                .fecha(LocalDate.of(2026, 5, 1))
                .estado("EMITIDA")
                .build();
        when(facturaRepository.buscarConFiltros(
                        null, 9L, 4L, desde, hasta, vencimiento, "emitida", PageRequest.of(0, 10)))
                .thenReturn(new PageImpl<>(List.of(factura), PageRequest.of(0, 10), 1));
        when(itemDonacionRepository.buscarPorDonaciones(List.of(20L))).thenReturn(List.of(item));
        ItemDonacionRespuesta itemRespuesta = new ItemDonacionRespuesta(
                30L, 4L, "Harina integral", "Harina", "kilos",
                new BigDecimal("2.500"), vencimiento);
        when(itemDonacionMapper.aRespuesta(item)).thenReturn(itemRespuesta);
        when(facturaMapper.aRespuesta(factura, List.of(
                        itemRespuesta)))
                .thenReturn(
                new com.group6.BancoAlimentos.factura.dto.FacturaRespuesta(
                        10L, "10", null, 9L, 20L, factura.getFecha(), "EMITIDA", null,
                        List.of(itemRespuesta)));

        var pagina = servicio.listar(null, 9L, 4L, desde, hasta, vencimiento, " emitida ", 0, 10);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("10", pagina.getContent().get(0).numero());
        assertEquals(20L, pagina.getContent().get(0).donacionId());
        assertEquals("Harina integral", pagina.getContent().get(0).detalles().get(0).nombreProducto());
        assertEquals(vencimiento, pagina.getContent().get(0).detalles().get(0).fechaVencimiento());
        verify(facturaRepository).buscarConFiltros(
                null, 9L, 4L, desde, hasta, vencimiento, "emitida", PageRequest.of(0, 10));
        verify(itemDonacionRepository).buscarPorDonaciones(List.of(20L));
    }

    @Test
    void laProyeccionSeIncluyeEnLaAuditoriaDeNegocio() {
        assertNotNull(Factura.class.getAnnotation(Audited.class));
    }
}
