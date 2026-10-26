package com.group6.BancoAlimentos.producto.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.producto.dto.ProductoRespuesta;
import com.group6.BancoAlimentos.producto.service.ConsultaProductoService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ConsultaProductoService consultaProductoService;

    public ProductoController(ConsultaProductoService consultaProductoService) {
        this.consultaProductoService = consultaProductoService;
    }

    @GetMapping
    public PaginaRespuesta<ProductoRespuesta> listar(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) Boolean activo,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<ProductoRespuesta> resultado =
                consultaProductoService.listar(nombre, activo, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }
}
