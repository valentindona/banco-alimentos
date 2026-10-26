package com.group6.BancoAlimentos.institucion.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.institucion.dto.InstitucionRespuesta;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.service.ConsultaInstitucionService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/instituciones")
public class InstitucionController {

    private final ConsultaInstitucionService consultaInstitucionService;

    public InstitucionController(ConsultaInstitucionService consultaInstitucionService) {
        this.consultaInstitucionService = consultaInstitucionService;
    }

    @GetMapping
    public PaginaRespuesta<InstitucionRespuesta> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) EstadoPago estadoPago,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<InstitucionRespuesta> resultado =
                consultaInstitucionService.listar(nombre, estadoPago, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }
}
