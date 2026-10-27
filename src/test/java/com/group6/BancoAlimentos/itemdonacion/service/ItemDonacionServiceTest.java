package com.group6.BancoAlimentos.itemdonacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ItemDonacionServiceTest {

    @Mock
    private ItemDonacionRepository itemDonacionRepository;

    @InjectMocks
    private ItemDonacionService servicio;

    @Test
    void guardaUnaLineaConCantidadDeTresDecimalesYSnapshotsDelProducto() {
        Donacion donacion = Donacion.builder()
                .id(10L)
                .fecha(LocalDate.now())
                .build();
        Producto producto = Producto.builder()
                .id(20L)
                .nombre("Leche")
                .tipoAlimento("Lácteo")
                .unidadMedida("litros")
                .activo(true)
                .build();
        when(itemDonacionRepository.save(any(ItemDonacion.class))).thenAnswer(invocacion -> {
            ItemDonacion item = invocacion.getArgument(0);
            item.setId(30L);
            return item;
        });

        ItemDonacion item = servicio.crear(
                donacion, producto, new BigDecimal("1.125"), LocalDate.now().plusDays(10));

        assertEquals(30L, item.getId());
        assertEquals(new BigDecimal("1.125"), item.getCantidad());
        assertEquals("Leche", item.getProductoNombre());
        assertEquals("Lácteo", item.getTipoAlimento());
        assertEquals("litros", item.getUnidadMedida());
        assertEquals(20L, item.getProducto().getId());
        assertEquals(donacion, item.getDonacion());
    }

    @Test
    void rechazaLineaInvalidaSinPersistir() {
        Donacion donacion = Donacion.builder().fecha(LocalDate.now()).build();
        Producto producto = Producto.builder()
                .id(20L).nombre("Leche").tipoAlimento("Lácteo").unidadMedida("litros").activo(true).build();

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(donacion, producto, BigDecimal.ZERO, LocalDate.now().plusDays(1)));
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(donacion, producto, BigDecimal.ONE, LocalDate.now()));
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(donacion, producto, BigDecimal.ONE, null));
        verify(itemDonacionRepository, org.mockito.Mockito.never()).save(any(ItemDonacion.class));
    }

    @Test
    void rechazaProductoInactivoOSinSnapshotCompleto() {
        Donacion donacion = Donacion.builder().fecha(LocalDate.now()).build();
        Producto inactivo = Producto.builder()
                .id(20L).nombre("Leche").tipoAlimento("Lácteo").unidadMedida("litros").activo(false).build();
        Producto sinTipo = Producto.builder()
                .id(21L).nombre("Histórico").unidadMedida("unidad").activo(true).build();

        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(donacion, inactivo, BigDecimal.ONE, LocalDate.now().plusDays(1)));
        assertThrows(
                ReglaNegocioException.class,
                () -> servicio.crear(donacion, sinTipo, BigDecimal.ONE, LocalDate.now().plusDays(1)));
        verify(itemDonacionRepository, org.mockito.Mockito.never()).save(any(ItemDonacion.class));
    }
}
