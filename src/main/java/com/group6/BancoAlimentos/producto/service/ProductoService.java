package com.group6.BancoAlimentos.producto.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.producto.dto.ProductoRespuesta;
import com.group6.BancoAlimentos.producto.dto.ProductoSolicitud;
import com.group6.BancoAlimentos.producto.mapper.ProductoMapper;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final ProductoMapper productoMapper;

    public ProductoService(ProductoRepository productoRepository, ProductoMapper productoMapper) {
        this.productoRepository = productoRepository;
        this.productoMapper = productoMapper;
    }

    @Transactional
    public ProductoRespuesta crear(ProductoSolicitud solicitud) {
        validar(solicitud);
        return productoMapper.aRespuesta(productoRepository.save(nuevoProducto(solicitud)));
    }

    @Transactional
    public Producto crearParaDonacion(ProductoSolicitud solicitud) {
        validar(solicitud);
        return productoRepository.save(nuevoProducto(solicitud));
    }

    @Transactional
    public ProductoRespuesta actualizar(Long productoId, ProductoSolicitud solicitud) {
        validar(solicitud);
        Producto producto = buscarPorId(productoId);
        producto.setNombre(solicitud.nombre().trim());
        producto.setDescripcion(normalizar(solicitud.descripcion()));
        producto.setTipoAlimento(solicitud.tipoAlimento().trim());
        producto.setUnidadMedida(solicitud.unidadMedida().trim());
        return productoMapper.aRespuesta(productoRepository.save(producto));
    }

    @Transactional(readOnly = true)
    public ProductoRespuesta buscarActivo(Long productoId) {
        Producto producto = productoRepository.findByIdAndActivoTrue(productoId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El producto no existe o está inactivo: " + productoId));
        return productoMapper.aRespuesta(producto);
    }

    @Transactional(readOnly = true)
    public Producto buscarModeloActivo(Long productoId) {
        return productoRepository.findByIdAndActivoTrue(productoId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El producto no existe o está inactivo: " + productoId));
    }

    @Transactional
    public Producto buscarModeloActivoBloqueado(Long productoId) {
        return productoRepository.buscarActivoBloqueado(productoId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El producto no existe o está inactivo: " + productoId));
    }

    @Transactional
    public void darDeBaja(Long productoId) {
        Producto producto = buscarPorId(productoId);
        if (producto.isActivo()) {
            producto.setActivo(false);
            productoRepository.save(producto);
        }
    }

    private Producto buscarPorId(Long productoId) {
        return productoRepository.findById(productoId)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el producto " + productoId));
    }

    private void validar(ProductoSolicitud solicitud) {
        if (solicitud == null
                || solicitud.nombre() == null
                || solicitud.nombre().isBlank()
                || solicitud.tipoAlimento() == null
                || solicitud.tipoAlimento().isBlank()
                || solicitud.unidadMedida() == null
                || solicitud.unidadMedida().isBlank()) {
            throw new ReglaNegocioException("Nombre, tipo de alimento y unidad de medida son obligatorios");
        }
    }

    private Producto nuevoProducto(ProductoSolicitud solicitud) {
        return Producto.builder()
                .nombre(solicitud.nombre().trim())
                .descripcion(normalizar(solicitud.descripcion()))
                .tipoAlimento(solicitud.tipoAlimento().trim())
                .unidadMedida(solicitud.unidadMedida().trim())
                .activo(true)
                .build();
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
