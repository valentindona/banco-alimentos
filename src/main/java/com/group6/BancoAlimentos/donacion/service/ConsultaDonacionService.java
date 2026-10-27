package com.group6.BancoAlimentos.donacion.service;

import com.group6.BancoAlimentos.Common.excepcion.RecursoNoEncontradoException;
import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.donacion.dto.DonacionRespuesta;
import com.group6.BancoAlimentos.donacion.mapper.DonacionMapper;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.donacion.repository.DonacionRepository;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import com.group6.BancoAlimentos.itemdonacion.mapper.ItemDonacionMapper;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaDonacionService {

    private final DonacionRepository donacionRepository;
    private final ItemDonacionRepository itemDonacionRepository;
    private final FacturaRepository facturaRepository;
    private final DonacionMapper donacionMapper;
    private final ItemDonacionMapper itemDonacionMapper;

    public ConsultaDonacionService(
            DonacionRepository donacionRepository,
            ItemDonacionRepository itemDonacionRepository,
            FacturaRepository facturaRepository,
            DonacionMapper donacionMapper,
            ItemDonacionMapper itemDonacionMapper) {
        this.donacionRepository = donacionRepository;
        this.itemDonacionRepository = itemDonacionRepository;
        this.facturaRepository = facturaRepository;
        this.donacionMapper = donacionMapper;
        this.itemDonacionMapper = itemDonacionMapper;
    }

    @Transactional(readOnly = true)
    public Page<DonacionRespuesta> listar(
            Long donanteId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long productoId,
            LocalDate vencimientoDesde,
            LocalDate vencimientoHasta,
            int pagina,
            int tamano) {
        Page<Donacion> donaciones = donacionRepository.buscarConFiltros(
                        donanteId,
                        fechaDesde,
                        fechaHasta,
                        productoId,
                        vencimientoDesde,
                        vencimientoHasta,
                        Paginacion.crear(pagina, tamano));
        List<Long> donacionIds =
                donaciones.getContent().stream().map(Donacion::getId).toList();
        if (donacionIds.isEmpty()) {
            return donaciones.map(this::aRespuesta);
        }
        Map<Long, List<ItemDonacion>> itemsPorDonacion =
                itemDonacionRepository.buscarPorDonaciones(donacionIds).stream()
                        .collect(Collectors.groupingBy(item -> item.getDonacion().getId()));
        Map<Long, Factura> facturasPorDonacion = facturaRepository.findByDonacionIdIn(donacionIds).stream()
                .collect(Collectors.toMap(Factura::getDonacionId, Function.identity()));
        return donaciones.map(donacion -> respuesta(
                donacion,
                itemsPorDonacion.getOrDefault(donacion.getId(), List.of()),
                exigirFactura(facturasPorDonacion.get(donacion.getId()), donacion.getId())));
    }

    @Transactional(readOnly = true)
    public DonacionRespuesta obtener(Long donacionId) {
        Donacion donacion = donacionRepository.findById(donacionId)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la donación " + donacionId));
        return aRespuesta(donacion);
    }

    @Transactional(readOnly = true)
    public List<ItemDonacionRespuesta> listarItems(Long donacionId) {
        if (!donacionRepository.existsById(donacionId)) {
            throw new RecursoNoEncontradoException("No se encontró la donación " + donacionId);
        }
        return itemDonacionRepository.findByDonacionIdOrderByIdAsc(donacionId).stream()
                .map(itemDonacionMapper::aRespuesta)
                .toList();
    }

    private DonacionRespuesta aRespuesta(Donacion donacion) {
        List<ItemDonacion> items =
                itemDonacionRepository.findByDonacionIdOrderByIdAsc(donacion.getId());
        Factura factura = facturaRepository.findByDonacionId(donacion.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "La donación confirmada no tiene un comprobante asociado: " + donacion.getId()));
        return respuesta(donacion, items, factura);
    }

    private DonacionRespuesta respuesta(
            Donacion donacion,
            List<ItemDonacion> items,
            Factura factura) {
        return donacionMapper.aRespuesta(donacion, items, factura);
    }

    private Factura exigirFactura(Factura factura, Long donacionId) {
        if (factura == null) {
            throw new IllegalStateException(
                    "La donación confirmada no tiene un comprobante asociado: " + donacionId);
        }
        return factura;
    }
}
