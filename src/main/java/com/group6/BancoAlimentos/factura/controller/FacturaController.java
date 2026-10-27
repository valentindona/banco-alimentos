package com.group6.BancoAlimentos.factura.controller;

import com.group6.BancoAlimentos.Common.respuesta.PaginaRespuesta;
import com.group6.BancoAlimentos.factura.dto.FacturaRespuesta;
import com.group6.BancoAlimentos.factura.dto.GenerarFacturaSolicitud;
import com.group6.BancoAlimentos.factura.mapper.FacturaMapper;
import com.group6.BancoAlimentos.factura.service.ConsultaFacturaService;
import com.group6.BancoAlimentos.factura.service.FacturaService;
import com.group6.BancoAlimentos.itemdonacion.mapper.ItemDonacionMapper;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facturas")
public class FacturaController {

    private final ConsultaFacturaService consultaFacturaService;
    private final FacturaService facturaService;
    private final FacturaMapper facturaMapper;
    private final ItemDonacionRepository itemDonacionRepository;
    private final ItemDonacionMapper itemDonacionMapper;

    public FacturaController(
            ConsultaFacturaService consultaFacturaService,
            FacturaService facturaService,
            FacturaMapper facturaMapper,
            ItemDonacionRepository itemDonacionRepository,
            ItemDonacionMapper itemDonacionMapper) {
        this.consultaFacturaService = consultaFacturaService;
        this.facturaService = facturaService;
        this.facturaMapper = facturaMapper;
        this.itemDonacionRepository = itemDonacionRepository;
        this.itemDonacionMapper = itemDonacionMapper;
    }

    @PostMapping("/generacion")
    public ResponseEntity<FacturaRespuesta> generar(@Valid @RequestBody GenerarFacturaSolicitud solicitud) {
        var resultado = facturaService.generarParaDonacion(solicitud.donacionId());
        var factura = resultado.factura();
        var detalles = factura.getDonacionId() == null
                ? java.util.List.<com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta>of()
                : itemDonacionRepository.findByDonacionIdOrderByIdAsc(factura.getDonacionId()).stream()
                        .map(itemDonacionMapper::aRespuesta)
                        .toList();
        return ResponseEntity.status(resultado.existente() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(facturaMapper.aRespuesta(factura, detalles));
    }

    @GetMapping
    public PaginaRespuesta<FacturaRespuesta> listar(
            @RequestParam(required = false) Long institucionId,
            @RequestParam(required = false) Long donanteId,
            @RequestParam(required = false) Long productoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaHasta,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fechaVencimiento,
            @RequestParam(required = false) String estado,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "20") int tamano) {
        Page<FacturaRespuesta> resultado =
                consultaFacturaService.listar(
                        institucionId, donanteId, productoId, fechaDesde, fechaHasta,
                        fechaVencimiento, estado, pagina, tamano);
        return PaginaRespuesta.desde(resultado);
    }
}
