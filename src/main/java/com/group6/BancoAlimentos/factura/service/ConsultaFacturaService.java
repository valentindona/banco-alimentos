package com.group6.BancoAlimentos.factura.service;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.factura.dto.FacturaRespuesta;
import com.group6.BancoAlimentos.factura.mapper.FacturaMapper;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import com.group6.BancoAlimentos.itemdonacion.mapper.ItemDonacionMapper;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaFacturaService {

    private final FacturaRepository facturaRepository;
    private final FacturaMapper facturaMapper;
    private final ItemDonacionRepository itemDonacionRepository;
    private final ItemDonacionMapper itemDonacionMapper;

    public ConsultaFacturaService(
            FacturaRepository facturaRepository,
            FacturaMapper facturaMapper,
            ItemDonacionRepository itemDonacionRepository,
            ItemDonacionMapper itemDonacionMapper) {
        this.facturaRepository = facturaRepository;
        this.facturaMapper = facturaMapper;
        this.itemDonacionRepository = itemDonacionRepository;
        this.itemDonacionMapper = itemDonacionMapper;
    }

    @Transactional(readOnly = true)
    public Page<FacturaRespuesta> listar(
            Long institucionId,
            Long donanteId,
            Long productoId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            LocalDate fechaVencimiento,
            String estado,
            int pagina,
            int tamano) {
        String filtroEstado = estado == null || estado.isBlank() ? null : estado.trim();
        Page<Factura> facturas = facturaRepository.buscarConFiltros(
                        institucionId,
                        donanteId,
                        productoId,
                        fechaDesde,
                        fechaHasta,
                        fechaVencimiento,
                        filtroEstado,
                        Paginacion.crear(pagina, tamano));
        List<Long> donacionIds = facturas.getContent().stream()
                .map(Factura::getDonacionId)
                .filter(donacionId -> donacionId != null)
                .toList();
        Map<Long, List<ItemDonacionRespuesta>> detallesPorDonacion;
        if (donacionIds.isEmpty()) {
            detallesPorDonacion = Map.of();
        } else {
            detallesPorDonacion = itemDonacionRepository.buscarPorDonaciones(donacionIds).stream()
                    .collect(Collectors.groupingBy(
                            item -> item.getDonacion().getId(),
                            Collectors.mapping(itemDonacionMapper::aRespuesta, Collectors.toList())));
        }
        return facturas.map(factura -> facturaMapper.aRespuesta(
                factura,
                factura.getDonacionId() == null
                        ? List.of()
                        : detallesPorDonacion.getOrDefault(factura.getDonacionId(), List.of())));
    }
}
