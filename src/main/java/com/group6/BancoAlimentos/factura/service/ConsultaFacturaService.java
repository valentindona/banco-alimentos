package com.group6.BancoAlimentos.factura.service;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.factura.dto.FacturaRespuesta;
import com.group6.BancoAlimentos.factura.mapper.FacturaMapper;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaFacturaService {

    private final FacturaRepository facturaRepository;
    private final FacturaMapper facturaMapper;

    public ConsultaFacturaService(FacturaRepository facturaRepository, FacturaMapper facturaMapper) {
        this.facturaRepository = facturaRepository;
        this.facturaMapper = facturaMapper;
    }

    @Transactional(readOnly = true)
    public Page<FacturaRespuesta> listar(
            Long institucionId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String estado,
            int pagina,
            int tamano) {
        String filtroEstado = estado == null || estado.isBlank() ? null : estado.trim();
        return facturaRepository.buscarConFiltros(
                        institucionId,
                        fechaDesde,
                        fechaHasta,
                        filtroEstado,
                        Paginacion.crear(pagina, tamano))
                .map(facturaMapper::aRespuesta);
    }
}
