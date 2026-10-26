package com.group6.BancoAlimentos.institucion.service;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.institucion.dto.InstitucionRespuesta;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaInstitucionService {

    private final InstitucionRepository institucionRepository;

    public ConsultaInstitucionService(InstitucionRepository institucionRepository) {
        this.institucionRepository = institucionRepository;
    }

    public boolean existeActiva(Long institucionId) {
        return institucionRepository.findById(institucionId)
                .map(institucion -> institucion.isActiva())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<InstitucionRespuesta> listar(
            String nombre, EstadoPago estadoPago, int pagina, int tamano) {
        String filtroNombre = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return institucionRepository.buscarConFiltros(
                        filtroNombre, estadoPago, Paginacion.crear(pagina, tamano))
                .map(this::aRespuesta);
    }

    private InstitucionRespuesta aRespuesta(Institucion institucion) {
        return new InstitucionRespuesta(
                institucion.getId(),
                institucion.getNombre(),
                institucion.getDomicilio(),
                institucion.getEstadoPago(),
                institucion.isActiva());
    }
}
