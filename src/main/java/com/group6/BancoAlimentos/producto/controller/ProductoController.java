package com.group6.BancoAlimentos.producto.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.producto.dto.ProductoRespuesta;
import com.group6.BancoAlimentos.producto.dto.ProductoSolicitud;
import com.group6.BancoAlimentos.producto.service.ConsultaProductoService;
import com.group6.BancoAlimentos.producto.service.ProductoService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ConsultaProductoService consultaProductoService;
    private final ProductoService productoService;

    public ProductoController(
            ConsultaProductoService consultaProductoService, ProductoService productoService) {
        this.consultaProductoService = consultaProductoService;
        this.productoService = productoService;
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

    @PostMapping
    public ResponseEntity<ProductoRespuesta> crear(@Valid @RequestBody ProductoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(solicitud));
    }

    @PutMapping("/{productoId}")
    public ProductoRespuesta actualizar(
            @PathVariable Long productoId, @Valid @RequestBody ProductoSolicitud solicitud) {
        return productoService.actualizar(productoId, solicitud);
    }

    @DeleteMapping("/{productoId}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Long productoId) {
        productoService.darDeBaja(productoId);
        return ResponseEntity.noContent().build();
    }
}
