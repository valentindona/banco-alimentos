package com.group6.BancoAlimentos.producto.modelo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class ProductoRepositoryTest {

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void persisteNombreUnidadDescripcionOpcionalYEstadoActivo() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Arroz")
                .unidadMedida("kg")
                .activo(true)
                .build());

        assertTrue(producto.getId() > 0);
        assertThrows(RuntimeException.class, () -> productoRepository.saveAndFlush(
                Producto.builder().unidadMedida("kg").activo(true).build()));
        assertThrows(RuntimeException.class, () -> productoRepository.saveAndFlush(
                Producto.builder().nombre("Sin unidad").activo(true).build()));
    }
}
