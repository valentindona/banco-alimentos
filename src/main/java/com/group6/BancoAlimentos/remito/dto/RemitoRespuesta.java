package com.group6.BancoAlimentos.remito.dto;

import com.group6.BancoAlimentos.detalleremito.dto.DetalleRemitoRespuesta;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import java.time.LocalDate;
import java.util.List;

public record RemitoRespuesta(
        Long id,
        Long institucionId,
        String institucionNombre,
        String institucionDomicilio,
        LocalDate fechaEntrega,
        EstadoRemito estado,
        List<DetalleRemitoRespuesta> detalles) {
}
