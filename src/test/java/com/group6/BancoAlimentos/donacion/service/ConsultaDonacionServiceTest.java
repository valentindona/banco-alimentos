package com.group6.BancoAlimentos.donacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.donacion.dto.DonacionRespuesta;
import com.group6.BancoAlimentos.donacion.mapper.DonacionMapper;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.donacion.repository.DonacionRepository;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import com.group6.BancoAlimentos.itemdonacion.mapper.ItemDonacionMapper;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultaDonacionServiceTest {

    @Mock
    private DonacionRepository donacionRepository;

    @Mock
    private ItemDonacionRepository itemDonacionRepository;

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private DonacionMapper donacionMapper;

    @Mock
    private ItemDonacionMapper itemDonacionMapper;

    @Test
    void filtraPorDonanteFechasProductoVencimientoYPaginaYDevuelveDetalleAsociado() {
        ConsultaDonacionService servicio = new ConsultaDonacionService(
                donacionRepository, itemDonacionRepository, facturaRepository, donacionMapper, itemDonacionMapper);
        LocalDate desde = LocalDate.of(2026, 1, 1);
        LocalDate hasta = LocalDate.of(2026, 12, 31);
        LocalDate vencimientoDesde = LocalDate.of(2027, 1, 1);
        LocalDate vencimientoHasta = LocalDate.of(2027, 3, 31);
        LocalDate fecha = LocalDate.of(2026, 5, 1);
        LocalDate vencimiento = LocalDate.of(2027, 2, 1);
        Donacion donacion = Donacion.builder()
                .id(20L)
                .donante(Donante.builder().id(9L).nombre("Donante").build())
                .fecha(fecha)
                .build();
        ItemDonacion item = ItemDonacion.builder()
                .id(30L)
                .donacion(donacion)
                .producto(Producto.builder().id(4L).nombre("Harina").build())
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
                .fecha(fecha)
                .estado("EMITIDA")
                .build();
        DonacionRespuesta respuesta = new DonacionRespuesta(20L, 9L, "Donante", fecha, List.of(
                new ItemDonacionRespuesta(
                        30L, 4L, "Harina integral", "Harina", "kilos",
                        new BigDecimal("2.500"), vencimiento)), null);
        PageRequest paginaSolicitada = PageRequest.of(1, 5);
        when(donacionRepository.buscarConFiltros(
                        9L, desde, hasta, 4L, vencimientoDesde, vencimientoHasta, paginaSolicitada))
                .thenReturn(new PageImpl<>(List.of(donacion), paginaSolicitada, 6));
        when(itemDonacionRepository.buscarPorDonaciones(List.of(20L))).thenReturn(List.of(item));
        when(facturaRepository.findByDonacionIdIn(List.of(20L))).thenReturn(List.of(factura));
        when(donacionMapper.aRespuesta(donacion, List.of(item), factura)).thenReturn(respuesta);

        var resultado = servicio.listar(
                9L, desde, hasta, 4L, vencimientoDesde, vencimientoHasta, 1, 5);

        assertEquals(6, resultado.getTotalElements());
        assertEquals("Harina integral", resultado.getContent().get(0).items().get(0).nombreProducto());
        assertEquals(vencimiento, resultado.getContent().get(0).items().get(0).fechaVencimiento());
        verify(donacionRepository).buscarConFiltros(
                9L, desde, hasta, 4L, vencimientoDesde, vencimientoHasta, paginaSolicitada);
        verify(facturaRepository).findByDonacionIdIn(List.of(20L));
    }
}
