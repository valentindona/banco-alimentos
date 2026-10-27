package com.group6.BancoAlimentos.itemdonacion.mapper;

import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import org.springframework.stereotype.Component;

@Component
public class ItemDonacionMapper {

    public ItemDonacionRespuesta aRespuesta(ItemDonacion item) {
        return new ItemDonacionRespuesta(
                item.getId(),
                item.getProducto().getId(),
                item.getProductoNombre(),
                item.getTipoAlimento(),
                item.getUnidadMedida(),
                item.getCantidad(),
                item.getFechaVencimiento());
    }
}
