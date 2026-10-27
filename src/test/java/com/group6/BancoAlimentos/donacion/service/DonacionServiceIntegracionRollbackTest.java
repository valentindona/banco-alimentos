package com.group6.BancoAlimentos.donacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.donacion.dto.DonacionSolicitud;
import com.group6.BancoAlimentos.donante.dto.DonanteSolicitud;
import com.group6.BancoAlimentos.donante.service.DonanteService;
import com.group6.BancoAlimentos.factura.service.FacturaService;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionSolicitud;
import com.group6.BancoAlimentos.itemdonacion.service.ItemDonacionService;
import com.group6.BancoAlimentos.producto.dto.ProductoSolicitud;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.service.ProductoService;
import com.group6.BancoAlimentos.stock.service.StockService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:donacion-rollback;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class DonacionServiceIntegracionRollbackTest {

    @Autowired
    private DonacionService donacionService;

    @Autowired
    private DonanteService donanteService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoBean
    private ItemDonacionService itemDonacionService;

    @MockitoBean
    private StockService stockService;

    @MockitoBean
    private FacturaService facturaService;

    @Test
    void revierteLaDonacionCuandoFallaElGuardadoDeUnItem() {
        var datos = crearDatos();
        when(itemDonacionService.crear(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new ReglaNegocioException("Fallo al guardar item"));

        verificarRollback(datos.donanteId(), datos.productoId());
    }

    @Test
    void revierteLaDonacionYItemsCuandoFallaElIngresoDeStock() {
        var datos = crearDatos();
        when(itemDonacionService.crear(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocacion -> crearItem(
                        invocacion.getArgument(0),
                        invocacion.getArgument(1),
                        invocacion.getArgument(2),
                        invocacion.getArgument(3),
                        invocacion.getArgument(4),
                        invocacion.getArgument(5),
                        invocacion.getArgument(6)));
        doThrow(new ReglaNegocioException("Fallo al ingresar stock"))
                .when(stockService).ingresar(any(Long.class), any(LocalDate.class), any(BigDecimal.class));

        verificarRollback(datos.donanteId(), datos.productoId());
    }

    @Test
    void revierteDonacionItemsYStockCuandoFallaLaEmisionDelComprobante() {
        var datos = crearDatos();
        when(itemDonacionService.crear(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(invocacion -> crearItem(
                        invocacion.getArgument(0),
                        invocacion.getArgument(1),
                        invocacion.getArgument(2),
                        invocacion.getArgument(3),
                        invocacion.getArgument(4),
                        invocacion.getArgument(5),
                        invocacion.getArgument(6)));
        doThrow(new ReglaNegocioException("Fallo al emitir comprobante"))
                .when(facturaService).emitir(any(Long.class), any(Long.class), any(LocalDate.class));

        verificarRollback(datos.donanteId(), datos.productoId());
    }

    private DatosDonacion crearDatos() {
        Long donanteId = donanteService.crear(new DonanteSolicitud(
                "Donante rollback " + UUID.randomUUID(), null, null)).id();
        Long productoId = productoService.crear(new ProductoSolicitud(
                "Producto rollback " + UUID.randomUUID(), null, "Granos", "kilos")).id();
        return new DatosDonacion(donanteId, productoId);
    }

    private void verificarRollback(Long donanteId, Long productoId) {
        int donacionesAntes = contar("donaciones");
        int itemsAntes = contar("items_donacion");
        int stockAntes = contar("stock");
        int facturasAntes = contar("facturas");
        LocalDate fecha = LocalDate.now();

        assertThrows(ReglaNegocioException.class, () -> donacionService.confirmar(new DonacionSolicitud(
                donanteId,
                fecha,
                List.of(new ItemDonacionSolicitud(
                        productoId, null, null, null, BigDecimal.ONE, fecha.plusDays(20))))));

        assertEquals(donacionesAntes, contar("donaciones"));
        assertEquals(itemsAntes, contar("items_donacion"));
        assertEquals(stockAntes, contar("stock"));
        assertEquals(facturasAntes, contar("facturas"));
    }

    private ItemDonacion crearItem(
            com.group6.BancoAlimentos.donacion.modelo.Donacion donacion,
            Producto producto,
            String nombre,
            String tipo,
            String unidad,
            BigDecimal cantidad,
            LocalDate vencimiento) {
        return ItemDonacion.builder()
                .donacion(donacion)
                .producto(producto)
                .productoNombre(nombre)
                .tipoAlimento(tipo)
                .unidadMedida(unidad)
                .cantidad(cantidad)
                .fechaVencimiento(vencimiento)
                .build();
    }

    private int contar(String tabla) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + tabla, Integer.class);
    }

    private record DatosDonacion(Long donanteId, Long productoId) {}
}
