package com.group6.BancoAlimentos.institucion.service;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.institucion.dto.InstitucionRespuesta;
import com.group6.BancoAlimentos.institucion.mapper.InstitucionMapper;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaInstitucionService {

    private final InstitucionRepository institucionRepository;
    private final InstitucionMapper institucionMapper;

    public ConsultaInstitucionService(
            InstitucionRepository institucionRepository, InstitucionMapper institucionMapper) {
        this.institucionRepository = institucionRepository;
        this.institucionMapper = institucionMapper;
    }

    public boolean existeActiva(Long institucionId) {
        return institucionRepository.findById(institucionId)
                .map(institucion -> institucion.isActiva())
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public Page<InstitucionRespuesta> listar(
            String nombre, String tipo, EstadoPago estadoPago, int pagina, int tamano) {
        String filtroNombre = nombre == null || nombre.isBlank() ? null : nombre.trim();
        String filtroTipo = tipo == null || tipo.isBlank() ? null : tipo.trim();
        return institucionRepository.buscarConFiltros(
                        filtroNombre,
                        filtroTipo,
                        estadoPago,
                        Paginacion.crear(pagina, tamano).withSort(Sort.by("id").ascending()))
                .map(institucionMapper::aRespuesta);
    }
}
