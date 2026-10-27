package com.group6.BancoAlimentos.producto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.producto.dto.ProductoSolicitud;
import com.group6.BancoAlimentos.producto.mapper.ProductoMapper;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductoDonacionServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Test
    void creaProductoManualConTipoYUnidadObligatorios() {
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> {
            Producto producto = invocacion.getArgument(0);
            producto.setId(40L);
            return producto;
        });
        ProductoService servicio = new ProductoService(productoRepository, new ProductoMapper());

        var respuesta = servicio.crear(new ProductoSolicitud(
                "Arroz integral", "Arroz largo", "Granos", "kilos"));

        assertEquals(40L, respuesta.id());
        assertEquals("Arroz integral", respuesta.nombre());
        assertEquals("Granos", respuesta.tipoAlimento());
        assertEquals("kilos", respuesta.unidadMedida());
        assertTrue(respuesta.activo());
    }

    @Test
    void rechazaTipoDeAlimentoVacioEnProductosNuevos() {
        ProductoService servicio = new ProductoService(productoRepository, new ProductoMapper());

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(new ProductoSolicitud("Arroz", null, " ", "kilos")));
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void permiteCompletarClasificacionDeUnProductoHistorico() {
        Producto historico = Producto.builder()
                .id(50L)
                .nombre("Producto histórico")
                .unidadMedida("unidad")
                .activo(true)
                .build();
        assertNull(historico.getTipoAlimento());
        when(productoRepository.findById(50L)).thenReturn(Optional.of(historico));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ProductoService servicio = new ProductoService(productoRepository, new ProductoMapper());

        var respuesta = servicio.actualizar(
                50L, new ProductoSolicitud("Producto histórico", null, "Legumbres", "bolsas"));

        assertEquals("Legumbres", respuesta.tipoAlimento());
        assertEquals("bolsas", respuesta.unidadMedida());
    }

    @Test
    void rechazaUsarUnProductoInactivo() {
        when(productoRepository.findByIdAndActivoTrue(55L)).thenReturn(Optional.empty());
        ProductoService servicio = new ProductoService(productoRepository, new ProductoMapper());

        assertThrows(ReglaNegocioException.class, () -> servicio.buscarActivo(55L));
    }

    @Test
    void actualizaProductoSinCambiarLaIdentidad() {
        Producto existente = Producto.builder()
                .id(60L)
                .nombre("Arroz")
                .unidadMedida("kilos")
                .tipoAlimento("Granos")
                .activo(true)
                .build();
        when(productoRepository.findById(60L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ProductoService servicio = new ProductoService(productoRepository, new ProductoMapper());

        var respuesta = servicio.actualizar(
                60L, new ProductoSolicitud("Arroz integral", "Largo fino", "Granos", "kilos"));

        assertEquals(60L, respuesta.id());
        assertEquals("Arroz integral", respuesta.nombre());
        assertEquals("Largo fino", respuesta.descripcion());
    }

    @Test
    void desactivaLogicamenteSinEliminarProductoConStockOHistorico() {
        Producto existente = Producto.builder()
                .id(70L)
                .nombre("Producto referenciado")
                .unidadMedida("cajas")
                .tipoAlimento("Envasados")
                .activo(true)
                .build();
        when(productoRepository.findById(70L)).thenReturn(Optional.of(existente));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        ProductoService servicio = new ProductoService(productoRepository, new ProductoMapper());

        servicio.darDeBaja(70L);

        assertFalse(existente.isActivo());
        assertEquals(70L, existente.getId());
        assertEquals("Producto referenciado", existente.getNombre());
        verify(productoRepository).save(existente);
        verify(productoRepository, never()).delete(any(Producto.class));
    }
}
