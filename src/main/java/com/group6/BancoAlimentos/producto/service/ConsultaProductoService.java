package com.group6.BancoAlimentos.producto.service;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.producto.dto.ProductoRespuesta;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaProductoService {

    private final ProductoRepository productoRepository;

    public ConsultaProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductoRespuesta> listar(String nombre, Boolean activo, int pagina, int tamano) {
        String filtroNombre = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return productoRepository.buscarConFiltros(
                        filtroNombre, activo, Paginacion.crear(pagina, tamano))
                .map(this::aRespuesta);
    }

    private ProductoRespuesta aRespuesta(Producto producto) {
        return new ProductoRespuesta(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getTipoAlimento(),
                producto.getUnidadMedida(),
                producto.isActivo());
    }
}
