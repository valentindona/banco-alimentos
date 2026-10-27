package com.group6.BancoAlimentos.institucion.mapper;

import com.group6.BancoAlimentos.institucion.dto.InstitucionRespuesta;
import com.group6.BancoAlimentos.institucion.dto.InstitucionSolicitud;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import org.springframework.stereotype.Component;

@Component
public class InstitucionMapper {

    public Institucion crearModelo(InstitucionSolicitud solicitud) {
        return Institucion.builder()
                .nombre(solicitud.nombre())
                .domicilio(solicitud.domicilio())
                .tipo(solicitud.tipo())
                .build();
    }

    public InstitucionRespuesta aRespuesta(Institucion institucion) {
        return new InstitucionRespuesta(
                institucion.getId(),
                institucion.getNombre(),
                institucion.getDomicilio(),
                institucion.getTipo(),
                institucion.getEstadoPago(),
                institucion.isActiva());
    }
}
