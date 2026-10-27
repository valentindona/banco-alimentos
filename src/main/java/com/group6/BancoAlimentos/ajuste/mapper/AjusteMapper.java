package com.group6.BancoAlimentos.ajuste.mapper;

import com.group6.BancoAlimentos.ajuste.dto.AjusteRespuesta;
import com.group6.BancoAlimentos.ajuste.modelo.Ajuste;
import org.springframework.stereotype.Component;

@Component
public class AjusteMapper {

    public AjusteRespuesta aRespuesta(Ajuste ajuste) {
        return new AjusteRespuesta(
                ajuste.getId(),
                ajuste.getStockId(),
                ajuste.getProductoId(),
                ajuste.getFechaVencimiento(),
                ajuste.getUsuarioId(),
                ajuste.getFechaHora(),
                ajuste.getTipo(),
                ajuste.getCantidadAnterior(),
                ajuste.getCantidadNueva(),
                ajuste.getMotivo());
    }
}
