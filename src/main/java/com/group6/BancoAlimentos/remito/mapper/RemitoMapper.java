package com.group6.BancoAlimentos.remito.mapper;

import com.group6.BancoAlimentos.detalleremito.dto.DetalleRemitoRespuesta;
import com.group6.BancoAlimentos.remito.dto.RemitoRespuesta;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RemitoMapper {

    public RemitoRespuesta aRespuesta(Remito remito, List<DetalleRemitoRespuesta> detalles) {
        return new RemitoRespuesta(
                remito.getId(),
                remito.getInstitucionId(),
                remito.getInstitucionNombre(),
                remito.getInstitucionDomicilio(),
                remito.getFechaEntrega(),
                remito.getEstado(),
                List.copyOf(detalles));
    }
}
