package com.group6.BancoAlimentos.producto.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ConsultaProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @Test
    void filtraPorNombreTipoUnidadEstadoYPagina() {
        ConsultaProductoService servicio = new ConsultaProductoService(productoRepository);
        Producto producto = Producto.builder()
                .id(4L)
                .nombre("Arroz integral")
                .tipoAlimento("Cereal")
                .unidadMedida("kg")
                .activo(true)
                .build();
        when(productoRepository.buscarConFiltros(
                        "arroz", "cereal", "kg", true, PageRequest.of(0, 2)))
                .thenReturn(new PageImpl<>(List.of(producto), PageRequest.of(0, 2), 1));

        var pagina = servicio.listar("arroz", "cereal", "kg", true, 0, 2);

        assertEquals(1, pagina.getTotalElements());
        assertEquals("Arroz integral", pagina.getContent().get(0).nombre());
        verify(productoRepository).buscarConFiltros(
                "arroz", "cereal", "kg", true, PageRequest.of(0, 2));
    }
}
