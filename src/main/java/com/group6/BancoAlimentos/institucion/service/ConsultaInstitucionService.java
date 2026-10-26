package com.group6.BancoAlimentos.institucion.service;

import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import org.springframework.stereotype.Service;

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
}
