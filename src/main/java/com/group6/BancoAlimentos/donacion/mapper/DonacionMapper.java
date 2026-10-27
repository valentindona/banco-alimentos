package com.group6.BancoAlimentos.donacion.mapper;

import com.group6.BancoAlimentos.donacion.dto.ComprobanteRespuesta;
import com.group6.BancoAlimentos.donacion.dto.DonacionRespuesta;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import com.group6.BancoAlimentos.itemdonacion.mapper.ItemDonacionMapper;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DonacionMapper {

    private final ItemDonacionMapper itemDonacionMapper;

    public DonacionMapper(ItemDonacionMapper itemDonacionMapper) {
        this.itemDonacionMapper = itemDonacionMapper;
    }

    public DonacionRespuesta aRespuesta(Donacion donacion, List<ItemDonacion> items, Factura factura) {
        List<ItemDonacionRespuesta> detalles = items.stream().map(itemDonacionMapper::aRespuesta).toList();
        return new DonacionRespuesta(
                donacion.getId(),
                donacion.getDonante().getId(),
                donacion.getDonante().getNombre(),
                donacion.getFecha(),
                detalles,
                factura == null ? null : new ComprobanteRespuesta(
                        factura.getId(),
                        factura.getNumero(),
                        donacion.getId(),
                        donacion.getDonante().getId(),
                        factura.getFecha(),
                        detalles));
    }
}
