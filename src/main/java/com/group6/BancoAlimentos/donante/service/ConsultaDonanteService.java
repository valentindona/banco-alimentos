package com.group6.BancoAlimentos.donante.service;

import com.group6.BancoAlimentos.Common.excepcion.RecursoNoEncontradoException;
import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.donante.dto.DonanteRespuesta;
import com.group6.BancoAlimentos.donante.mapper.DonanteMapper;
import com.group6.BancoAlimentos.donante.repository.DonanteRepository;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaDonanteService {

    private final DonanteRepository donanteRepository;
    private final DonanteMapper donanteMapper;

    public ConsultaDonanteService(DonanteRepository donanteRepository, DonanteMapper donanteMapper) {
        this.donanteRepository = donanteRepository;
        this.donanteMapper = donanteMapper;
    }

    @Transactional(readOnly = true)
    public Page<DonanteRespuesta> listar(String nombre, Boolean activo, int pagina, int tamano) {
        String filtroNombre = nombre == null || nombre.isBlank() ? null : nombre.trim();
        return donanteRepository.buscarConFiltros(
                        filtroNombre, activo, Paginacion.crear(pagina, tamano))
                .map(donanteMapper::aRespuesta);
    }

    @Transactional(readOnly = true)
    public DonanteRespuesta obtener(Long donanteId) {
        return donanteRepository.findById(donanteId)
                .map(donanteMapper::aRespuesta)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el donante " + donanteId));
    }
}
