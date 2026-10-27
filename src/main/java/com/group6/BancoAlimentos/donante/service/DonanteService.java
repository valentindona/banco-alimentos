package com.group6.BancoAlimentos.donante.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.donante.dto.DonanteRespuesta;
import com.group6.BancoAlimentos.donante.dto.DonanteSolicitud;
import com.group6.BancoAlimentos.donante.mapper.DonanteMapper;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.donante.repository.DonanteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DonanteService {

    private final DonanteRepository donanteRepository;
    private final DonanteMapper donanteMapper;

    public DonanteService(DonanteRepository donanteRepository, DonanteMapper donanteMapper) {
        this.donanteRepository = donanteRepository;
        this.donanteMapper = donanteMapper;
    }

    @Transactional
    public DonanteRespuesta crear(DonanteSolicitud solicitud) {
        validar(solicitud);
        Donante donante = Donante.builder()
                .nombre(solicitud.nombre().trim())
                .correo(normalizar(solicitud.correo()))
                .telefono(normalizar(solicitud.telefono()))
                .activo(true)
                .build();
        return donanteMapper.aRespuesta(donanteRepository.save(donante));
    }

    @Transactional
    public DonanteRespuesta actualizar(Long donanteId, DonanteSolicitud solicitud) {
        validar(solicitud);
        Donante donante = buscarPorId(donanteId);
        if (!donante.isActivo()) {
            throw new ReglaNegocioException("No se puede reactivar ni modificar un donante dado de baja");
        }

        donante.setNombre(solicitud.nombre().trim());
        donante.setCorreo(normalizar(solicitud.correo()));
        donante.setTelefono(normalizar(solicitud.telefono()));
        return donanteMapper.aRespuesta(donanteRepository.save(donante));
    }

    @Transactional(readOnly = true)
    public DonanteRespuesta buscarActivo(Long donanteId) {
        Donante donante = donanteRepository.findByIdAndActivoTrue(donanteId)
                .orElseThrow(() -> new ReglaNegocioException(
                        "El donante no existe o está dado de baja: " + donanteId));
        return donanteMapper.aRespuesta(donante);
    }

    @Transactional
    public void darDeBaja(Long donanteId) {
        Donante donante = buscarPorId(donanteId);
        if (donante.isActivo()) {
            donante.setActivo(false);
            donanteRepository.save(donante);
        }
    }

    private Donante buscarPorId(Long donanteId) {
        return donanteRepository.findById(donanteId)
                .orElseThrow(() -> new ReglaNegocioException("No se encontró el donante " + donanteId));
    }

    private void validar(DonanteSolicitud solicitud) {
        if (solicitud == null || solicitud.nombre() == null || solicitud.nombre().isBlank()) {
            throw new ReglaNegocioException("El nombre del donante es obligatorio");
        }
    }

    private String normalizar(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return valor.trim();
    }
}
