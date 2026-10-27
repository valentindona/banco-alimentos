package com.group6.BancoAlimentos.factura.mapper;

import com.group6.BancoAlimentos.factura.dto.FacturaRespuesta;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FacturaMapper {

    public FacturaRespuesta aRespuesta(Factura factura) {
        return aRespuesta(factura, List.of());
    }

    public FacturaRespuesta aRespuesta(Factura factura, List<ItemDonacionRespuesta> detalles) {
        return new FacturaRespuesta(
                factura.getId(),
                factura.getNumero(),
                factura.getInstitucionId(),
                factura.getDonanteId(),
                factura.getDonacionId(),
                factura.getFecha(),
                factura.getEstado(),
                factura.getImporte(),
                detalles);
    }
}
