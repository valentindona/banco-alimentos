package com.group6.BancoAlimentos.factura.mapper;

import com.group6.BancoAlimentos.factura.dto.FacturaRespuesta;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import org.springframework.stereotype.Component;

@Component
public class FacturaMapper {

    public FacturaRespuesta aRespuesta(Factura factura) {
        return new FacturaRespuesta(
                factura.getId(),
                factura.getNumero(),
                factura.getInstitucionId(),
                factura.getDonanteId(),
                factura.getDonacionId(),
                factura.getFecha(),
                factura.getEstado(),
                factura.getImporte());
    }
}
