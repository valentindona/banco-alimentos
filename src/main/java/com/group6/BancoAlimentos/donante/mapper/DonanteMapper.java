package com.group6.BancoAlimentos.donante.mapper;

import com.group6.BancoAlimentos.donante.dto.DonanteRespuesta;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import org.springframework.stereotype.Component;

@Component
public class DonanteMapper {

    public DonanteRespuesta aRespuesta(Donante donante) {
        return new DonanteRespuesta(
                donante.getId(),
                donante.getNombre(),
                donante.getCorreo(),
                donante.getTelefono(),
                donante.isActivo());
    }
}
