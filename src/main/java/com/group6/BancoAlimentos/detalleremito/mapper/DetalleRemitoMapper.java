package com.group6.BancoAlimentos.detalleremito.mapper;

import com.group6.BancoAlimentos.detalleremito.dto.DetalleRemitoRespuesta;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import org.springframework.stereotype.Component;

@Component
public class DetalleRemitoMapper {

    public DetalleRemitoRespuesta aRespuesta(DetalleRemito detalle) {
        return new DetalleRemitoRespuesta(
                detalle.getId(),
                detalle.getProductoId(),
                detalle.getProductoNombre(),
                detalle.getStockId(),
                detalle.getFechaVencimiento(),
                detalle.getCantidad());
    }
}
