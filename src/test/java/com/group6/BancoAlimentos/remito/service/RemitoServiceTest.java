package com.group6.BancoAlimentos.remito.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RemitoServiceTest {

    @Mock
    private RemitoRepository remitoRepository;

    @Mock
    private DetalleRemitoRepository detalleRepository;

    @Mock
    private InstitucionRepository institucionRepository;

    @Mock
    private StockRepository stockRepository;

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private RemitoService servicio;

    private Institucion institucion;
    private Producto producto;
    private Stock stock;
    private Remito remito;

    @BeforeEach
    void prepararDatos() {
        institucion = Institucion.builder()
                .id(5L)
                .nombre("Institución")
                .domicilio("Calle 123")
                .estadoPago(EstadoPago.EN_DEUDA)
                .activa(true)
                .build();
        producto = Producto.builder().id(8L).nombre("Arroz").unidadMedida("kg").activo(true).build();
        stock = Stock.builder()
                .id(11L)
                .productoId(8L)
                .fechaVencimiento(LocalDate.now().plusDays(20))
                .cantidadDisponible(new BigDecimal("10.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build();
        remito = Remito.builder()
                .id(13L)
                .institucionId(5L)
                .institucionNombre("Institución")
                .institucionDomicilio("Calle 123")
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(41L)
                .build();
    }

    @Test
    void permiteCrearBorradorParaInstitucionEnDeuda() {
        when(institucionRepository.findById(5L)).thenReturn(Optional.of(institucion));
        when(remitoRepository.save(any(Remito.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        Remito creado = servicio.crearBorrador(5L, LocalDate.now().plusDays(1), 41L);

        assertEquals("Institución", creado.getInstitucionNombre());
        assertEquals(EstadoRemito.BORRADOR, creado.getEstado());
        verify(remitoRepository).save(any(Remito.class));
    }

    @Test
    void rechazaInstitucionInexistenteInactivaOIncompleta() {
        when(institucionRepository.findById(5L)).thenReturn(Optional.empty());
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crearBorrador(5L, LocalDate.now().plusDays(1), 41L));

        institucion.setActiva(false);
        when(institucionRepository.findById(5L)).thenReturn(Optional.of(institucion));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crearBorrador(5L, LocalDate.now().plusDays(1), 41L));

        institucion.setActiva(true);
        institucion.setDomicilio(" ");
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crearBorrador(5L, LocalDate.now().plusDays(1), 41L));
    }

    @Test
    void rechazaFechaDeEntregaNoFutura() {
        when(institucionRepository.findById(5L)).thenReturn(Optional.of(institucion));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.crearBorrador(5L, LocalDate.now(), 41L));
    }

    @Test
    void rechazaStockVencidoHoyOAntesYSinVencimiento() {
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));
        when(stockRepository.findById(11L)).thenReturn(Optional.of(stock));

        stock.setFechaVencimiento(LocalDate.now());
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(13L, 11L, BigDecimal.ONE));

        stock.setFechaVencimiento(LocalDate.now().minusDays(1));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(13L, 11L, BigDecimal.ONE));

        stock.setFechaVencimiento(null);
        stock.setEstado(EstadoStock.SIN_VENCIMIENTO);
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(13L, 11L, BigDecimal.ONE));
    }

    @Test
    void rechazaCantidadNoPositivaYExistenciaDuplicada() {
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));
        when(stockRepository.findById(11L)).thenReturn(Optional.of(stock));
        when(productoRepository.findById(8L)).thenReturn(Optional.of(producto));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(13L, 11L, BigDecimal.ZERO));

        when(detalleRepository.findByRemitoIdOrderByStockIdAsc(13L))
                .thenReturn(java.util.List.of(DetalleRemito.builder().id(17L).stockId(11L).build()));
        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(13L, 11L, BigDecimal.ONE));
    }

    @Test
    void rechazaAgregarDetalleAUnRemitoNoBorrador() {
        remito.setEstado(EstadoRemito.CONFIRMADO);
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.agregarDetalle(13L, 11L, BigDecimal.ONE));
    }

    @Test
    void actualizaBorradorYRefrescaElSnapshotInstitucional() {
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));
        when(institucionRepository.findById(5L)).thenReturn(Optional.of(institucion));
        when(remitoRepository.save(any(Remito.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        LocalDate nuevaFecha = LocalDate.now().plusDays(5);

        Remito actualizado = servicio.actualizarBorrador(13L, 5L, nuevaFecha);

        assertEquals("Institución", actualizado.getInstitucionNombre());
        assertEquals("Calle 123", actualizado.getInstitucionDomicilio());
        assertEquals(nuevaFecha, actualizado.getFechaEntrega());
        verify(remitoRepository).save(remito);
    }

    @Test
    void noModificaNiEliminaRemitosConfirmados() {
        remito.setEstado(EstadoRemito.CONFIRMADO);
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));

        assertThrows(ReglaNegocioException.class,
                () -> servicio.actualizarBorrador(13L, 5L, LocalDate.now().plusDays(5)));
        assertThrows(ReglaNegocioException.class, () -> servicio.eliminarBorrador(13L));
        verify(remitoRepository, never()).delete(any(Remito.class));
    }

    @Test
    void actualizaArticuloDeUnBorradorValidandoExistenciaYProducto() {
        DetalleRemito detalle = DetalleRemito.builder()
                .id(17L)
                .remitoId(13L)
                .stockId(11L)
                .productoId(8L)
                .fechaVencimiento(stock.getFechaVencimiento())
                .productoNombre("Arroz")
                .cantidad(new BigDecimal("2.000"))
                .build();
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));
        when(detalleRepository.findByIdAndRemitoId(17L, 13L)).thenReturn(Optional.of(detalle));
        when(stockRepository.findById(11L)).thenReturn(Optional.of(stock));
        when(productoRepository.findById(8L)).thenReturn(Optional.of(producto));
        when(detalleRepository.findByRemitoIdOrderByStockIdAsc(13L)).thenReturn(java.util.List.of(detalle));
        when(detalleRepository.save(any(DetalleRemito.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        DetalleRemito actualizado = servicio.actualizarDetalle(13L, 17L, 11L, new BigDecimal("3.000"));

        assertEquals(new BigDecimal("3.000"), actualizado.getCantidad());
        verify(detalleRepository).save(detalle);
    }

    @Test
    void eliminaSoloArticulosDeRemitosBorrador() {
        DetalleRemito detalle = DetalleRemito.builder().id(17L).remitoId(13L).build();
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));
        when(detalleRepository.findByIdAndRemitoId(17L, 13L)).thenReturn(Optional.of(detalle));

        servicio.eliminarDetalle(13L, 17L);

        verify(detalleRepository).delete(detalle);
    }

    @Test
    void eliminaBorradorYsusArticulosSinAfectarElStock() {
        DetalleRemito detalle = DetalleRemito.builder().id(17L).remitoId(13L).build();
        when(remitoRepository.buscarPorIdBloqueado(13L)).thenReturn(Optional.of(remito));
        when(detalleRepository.findByRemitoIdOrderByStockIdAsc(13L)).thenReturn(java.util.List.of(detalle));

        servicio.eliminarBorrador(13L);

        verify(detalleRepository).deleteAll(java.util.List.of(detalle));
        verify(remitoRepository).delete(remito);
    }
}
