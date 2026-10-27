package com.group6.BancoAlimentos.donacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.donacion.dto.DonacionSolicitud;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.donacion.repository.DonacionRepository;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.donante.service.DonanteService;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.service.FacturaService;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.service.ItemDonacionService;
import com.group6.BancoAlimentos.producto.dto.ProductoSolicitud;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.service.ProductoService;
import com.group6.BancoAlimentos.stock.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.group6.BancoAlimentos.donacion.mapper.DonacionMapper;
import com.group6.BancoAlimentos.itemdonacion.mapper.ItemDonacionMapper;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionSolicitud;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DonacionServiceTest {

    @Mock
    private DonacionRepository donacionRepository;

    @Mock
    private DonanteService donanteService;

    @Mock
    private ProductoService productoService;

    @Mock
    private ItemDonacionService itemDonacionService;

    @Mock
    private StockService stockService;

    @Mock
    private FacturaService facturaService;

    @Test
    void confirmaLineasConSnapshotsSinMutarElProductoDelCatalogo() {
        LocalDate fecha = LocalDate.now();
        LocalDate vencimiento = fecha.plusDays(20);
        Producto producto = crearProducto(7L, "Leche", "Lácteo", "litros");
        Donacion donacion = Donacion.builder().id(10L).fecha(fecha).build();
        ItemDonacion item = ItemDonacion.builder()
                .id(11L)
                .donacion(donacion)
                .producto(producto)
                .productoNombre("Leche de productor")
                .tipoAlimento("Leche fresca")
                .unidadMedida("botellas")
                .cantidad(new BigDecimal("2.500"))
                .fechaVencimiento(vencimiento)
                .build();
        Factura factura = Factura.builder()
                .id(12L).numero("12").estado("EMITIDA").donacionId(10L).donanteId(5L).fecha(fecha).build();
        prepararMocks(producto, donacion, item, factura);
        DonacionService servicio = crearServicio();

        var respuesta = servicio.confirmar(solicitud(fecha, item(7L, "Leche de productor", "Leche fresca",
                "botellas", new BigDecimal("2.500"), vencimiento)));

        assertEquals(1, respuesta.items().size());
        assertEquals("Leche de productor", respuesta.items().get(0).nombreProducto());
        assertEquals("Leche fresca", respuesta.items().get(0).tipoAlimento());
        assertEquals("botellas", respuesta.items().get(0).unidadMedida());
        assertEquals("Lácteo", producto.getTipoAlimento());
        assertEquals("litros", producto.getUnidadMedida());
        verify(productoService, never()).crearParaDonacion(any(ProductoSolicitud.class));
        verify(stockService).ingresar(7L, vencimiento, new BigDecimal("2.500"));
        verify(facturaService).emitir(5L, 10L, fecha);
    }

    @Test
    void creaProductoManualConUnaUnidadBaseSinConversiones() {
        LocalDate fecha = LocalDate.now();
        LocalDate vencimiento = fecha.plusDays(15);
        Producto producto = crearProducto(8L, "Arroz", "Granos", "kilos");
        Donacion donacion = Donacion.builder().id(20L).fecha(fecha).build();
        ItemDonacion item = ItemDonacion.builder()
                .id(21L)
                .donacion(donacion)
                .producto(producto)
                .productoNombre("Arroz")
                .tipoAlimento("Granos")
                .unidadMedida("kilos")
                .cantidad(new BigDecimal("1.250"))
                .fechaVencimiento(vencimiento)
                .build();
        Factura factura = Factura.builder()
                .id(22L).numero("22").estado("EMITIDA").donacionId(20L).donanteId(5L).fecha(fecha).build();
        when(donanteService.obtenerModeloActivo(5L)).thenReturn(crearDonante());
        donacion.setDonante(crearDonante());
        when(productoService.crearParaDonacion(new ProductoSolicitud("Arroz", null, "Granos", "kilos")))
                .thenReturn(producto);
        when(donacionRepository.save(any(Donacion.class))).thenReturn(donacion);
        item.setProducto(producto);
        when(itemDonacionService.crear(
                        donacion, producto, "Arroz", "Granos", "kilos", new BigDecimal("1.250"), vencimiento))
                .thenReturn(item);
        when(facturaService.emitir(5L, 20L, fecha)).thenReturn(factura);
        DonacionService servicio = crearServicio();

        var respuesta = servicio.confirmar(solicitud(
                fecha, item(null, "Arroz", "Granos", "kilos", new BigDecimal("1.250"), vencimiento)));

        assertEquals(8L, respuesta.items().get(0).productoId());
        assertEquals("kilos", respuesta.items().get(0).unidadMedida());
        verify(productoService).crearParaDonacion(new ProductoSolicitud("Arroz", null, "Granos", "kilos"));
        verify(stockService).ingresar(8L, vencimiento, new BigDecimal("1.250"));
    }

    @Test
    void rechazaDonanteInactivoAntesDePersistir() {
        LocalDate fecha = LocalDate.now();
        when(donanteService.obtenerModeloActivo(5L)).thenThrow(new ReglaNegocioException("Donante inactivo"));
        DonacionService servicio = crearServicio();

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.confirmar(solicitud(fecha, item(
                        7L, null, null, null, BigDecimal.ONE, fecha.plusDays(1)))));
        verify(donacionRepository, never()).save(any(Donacion.class));
    }

    @Test
    void validaTodasLasLineasAntesDePersistirProductosODonacion() {
        LocalDate fecha = LocalDate.now();
        DonacionService servicio = crearServicio();

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.confirmar(solicitud(
                        fecha,
                        item(null, "Harina", null, "kilos", BigDecimal.ONE, fecha.plusDays(10)))));

        verify(productoService, never()).crearParaDonacion(any(ProductoSolicitud.class));
        verify(donacionRepository, never()).save(any(Donacion.class));
    }

    @Test
    void propagaFallasDeStockYFactura() {
        LocalDate fecha = LocalDate.now();
        LocalDate vencimiento = fecha.plusDays(10);
        Producto producto = crearProducto(7L, "Leche", "Lácteo", "litros");
        Donacion donacion = Donacion.builder().id(10L).fecha(fecha).build();
        ItemDonacion item = ItemDonacion.builder()
                .id(11L).donacion(donacion).productoNombre("Leche").tipoAlimento("Lácteo")
                .unidadMedida("litros").cantidad(BigDecimal.ONE).fechaVencimiento(vencimiento).build();
        prepararMocks(producto, donacion, item, null);
        org.mockito.Mockito.doThrow(new ReglaNegocioException("Fallo de stock"))
                .when(stockService).ingresar(7L, vencimiento, new BigDecimal("1.000"));
        DonacionService servicio = crearServicio();

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.confirmar(solicitud(
                        fecha, item(7L, null, null, null, BigDecimal.ONE, vencimiento))));
    }

    @Test
    void propagaFallaAlEmitirComprobante() {
        LocalDate fecha = LocalDate.now();
        LocalDate vencimiento = fecha.plusDays(10);
        Producto producto = crearProducto(7L, "Leche", "Lácteo", "litros");
        Donacion donacion = Donacion.builder().id(10L).fecha(fecha).build();
        ItemDonacion item = ItemDonacion.builder()
                .id(11L).donacion(donacion).producto(producto).productoNombre("Leche").tipoAlimento("Lácteo")
                .unidadMedida("litros").cantidad(BigDecimal.ONE).fechaVencimiento(vencimiento).build();
        prepararMocks(producto, donacion, item, null);
        org.mockito.Mockito.doThrow(new ReglaNegocioException("Fallo de factura"))
                .when(facturaService).emitir(5L, 10L, fecha);
        DonacionService servicio = crearServicio();

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.confirmar(solicitud(
                        fecha, item(7L, null, null, null, BigDecimal.ONE, vencimiento))));
    }

    private void prepararMocks(
            Producto producto, Donacion donacion, ItemDonacion item, Factura factura) {
        when(donanteService.obtenerModeloActivo(5L)).thenReturn(crearDonante());
        donacion.setDonante(crearDonante());
        when(productoService.buscarModeloActivo(7L)).thenReturn(producto);
        when(donacionRepository.save(any(Donacion.class))).thenReturn(donacion);
        when(itemDonacionService.crear(
                        any(Donacion.class), any(Producto.class), any(), any(), any(), any(), any()))
                .thenReturn(item);
        if (factura != null) {
            when(facturaService.emitir(5L, donacion.getId(), donacion.getFecha())).thenReturn(factura);
        }
    }

    private DonacionService crearServicio() {
        return new DonacionService(
                donacionRepository,
                donanteService,
                productoService,
                itemDonacionService,
                stockService,
                facturaService,
                new DonacionMapper(new ItemDonacionMapper()));
    }

    private DonacionSolicitud solicitud(LocalDate fecha, ItemDonacionSolicitud... items) {
        return new DonacionSolicitud(5L, fecha, List.of(items));
    }

    private ItemDonacionSolicitud item(
            Long productoId,
            String nombre,
            String tipo,
            String unidad,
            BigDecimal cantidad,
            LocalDate vencimiento) {
        return new ItemDonacionSolicitud(productoId, nombre, tipo, unidad, cantidad, vencimiento);
    }

    private Producto crearProducto(Long id, String nombre, String tipo, String unidad) {
        return Producto.builder()
                .id(id)
                .nombre(nombre)
                .tipoAlimento(tipo)
                .unidadMedida(unidad)
                .activo(true)
                .build();
    }

    private Donante crearDonante() {
        return Donante.builder().id(5L).nombre("Donante").activo(true).build();
    }
}
