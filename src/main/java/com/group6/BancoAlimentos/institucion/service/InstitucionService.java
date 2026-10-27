package com.group6.BancoAlimentos.institucion.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.institucion.dto.InstitucionRespuesta;
import com.group6.BancoAlimentos.institucion.dto.InstitucionSolicitud;
import com.group6.BancoAlimentos.institucion.mapper.InstitucionMapper;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InstitucionService {

    private final InstitucionRepository institucionRepository;
    private final InstitucionMapper institucionMapper;

    public InstitucionService(
            InstitucionRepository institucionRepository, InstitucionMapper institucionMapper) {
        this.institucionRepository = institucionRepository;
        this.institucionMapper = institucionMapper;
    }

    @Transactional
    public InstitucionRespuesta crear(InstitucionSolicitud solicitud) {
        String nombre = normalizar(solicitud.nombre());
        if (institucionRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ReglaNegocioException("Ya existe una institución con el nombre " + nombre);
        }

        Institucion institucion = institucionMapper.crearModelo(solicitud);
        institucion.setNombre(nombre);
        institucion.setDomicilio(normalizar(solicitud.domicilio()));
        institucion.setTipo(normalizar(solicitud.tipo()));
        institucion.setEstadoPago(EstadoPago.AL_DIA);
        institucion.setActiva(true);
        return institucionMapper.aRespuesta(institucionRepository.save(institucion));
    }

    @Transactional
    public InstitucionRespuesta actualizar(Long institucionId, InstitucionSolicitud solicitud) {
        Institucion institucion = obtenerActiva(institucionId);
        String nombre = normalizar(solicitud.nombre());
        if (institucionRepository.existsByNombreIgnoreCaseAndIdNot(nombre, institucionId)) {
            throw new ReglaNegocioException("Ya existe una institución con el nombre " + nombre);
        }

        institucion.setNombre(nombre);
        institucion.setDomicilio(normalizar(solicitud.domicilio()));
        institucion.setTipo(normalizar(solicitud.tipo()));
        return institucionMapper.aRespuesta(institucionRepository.save(institucion));
    }

    @Transactional(readOnly = true)
    public InstitucionRespuesta buscarActiva(Long institucionId) {
        return institucionMapper.aRespuesta(obtenerActiva(institucionId));
    }

    @Transactional
    public InstitucionRespuesta cambiarEstadoPago(Long institucionId, EstadoPago estadoPago) {
        Institucion institucion = obtenerActiva(institucionId);
        institucion.setEstadoPago(estadoPago);
        return institucionMapper.aRespuesta(institucionRepository.save(institucion));
    }

    @Transactional
    public void darDeBaja(Long institucionId) {
        Institucion institucion = institucionRepository.findById(institucionId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "No se encontró la institución " + institucionId));
        if (institucion.isActiva()) {
            institucion.setActiva(false);
            institucionRepository.save(institucion);
        }
    }

    private Institucion obtenerActiva(Long institucionId) {
        return institucionRepository.findByIdAndActivaTrue(institucionId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "La institución no existe o está dada de baja: " + institucionId));
    }

    private String normalizar(String valor) {
        return valor.trim();
    }
}
