package com.group6.BancoAlimentos.producto.mapper;

import com.group6.BancoAlimentos.producto.dto.ProductoRespuesta;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    public ProductoRespuesta aRespuesta(Producto producto) {
        return new ProductoRespuesta(
                producto.getId(),
                producto.getNombre(),
                producto.getDescripcion(),
                producto.getTipoAlimento(),
                producto.getUnidadMedida(),
                producto.isActivo());
    }
}
