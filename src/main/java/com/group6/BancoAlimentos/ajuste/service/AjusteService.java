package com.group6.BancoAlimentos.ajuste.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.Common.excepcion.SolicitudInvalidaException;
import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.ajuste.dto.AjusteRespuesta;
import com.group6.BancoAlimentos.ajuste.dto.AjusteSolicitud;
import com.group6.BancoAlimentos.ajuste.mapper.AjusteMapper;
import com.group6.BancoAlimentos.ajuste.modelo.Ajuste;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import com.group6.BancoAlimentos.ajuste.repository.AjusteRepository;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AjusteService {

    private final StockRepository stockRepository;
    private final AjusteRepository ajusteRepository;
    private final AjusteMapper ajusteMapper;

    public AjusteService(StockRepository stockRepository, AjusteRepository ajusteRepository) {
        this(stockRepository, ajusteRepository, new AjusteMapper());
    }

    @Autowired
    public AjusteService(
            StockRepository stockRepository,
            AjusteRepository ajusteRepository,
            AjusteMapper ajusteMapper) {
        this.stockRepository = stockRepository;
        this.ajusteRepository = ajusteRepository;
        this.ajusteMapper = ajusteMapper;
    }

    @Transactional
    public AjusteRespuesta registrar(AjusteSolicitud solicitud, UsuarioAutenticado usuario) {
        validarSolicitud(solicitud, usuario);
        String motivo = solicitud.motivo().strip();
        Stock stock = stockRepository.buscarTodosPorIdBloqueadosOrdenados(List.of(solicitud.stockId())).stream()
                .findFirst()
                .orElseThrow(() -> new ReglaNegocioException(
                        "No se encontró el lote de stock " + solicitud.stockId()));
        BigDecimal anterior = stock.getCantidadDisponible();
        BigDecimal nueva = calcularNuevaCantidad(solicitud.tipo(), solicitud.cantidad(), anterior);

        Ajuste ajuste = new Ajuste(
                stock.getId(),
                stock.getProductoId(),
                stock.getFechaVencimiento(),
                usuario.usuarioId(),
                Instant.now(),
                solicitud.tipo(),
                anterior,
                nueva,
                motivo);
        stock.setCantidadDisponible(nueva);
        stockRepository.saveAndFlush(stock);
        return ajusteMapper.aRespuesta(ajusteRepository.save(ajuste));
    }

    @Transactional(readOnly = true)
    public Page<AjusteRespuesta> listar(
            Long productoId,
            LocalDate fechaVencimiento,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            Long usuarioId,
            TipoAjuste tipo,
            int pagina,
            int tamano) {
        if (fechaDesde != null && fechaHasta != null && fechaDesde.isAfter(fechaHasta)) {
            throw new SolicitudInvalidaException("fechaDesde no puede ser posterior a fechaHasta.");
        }
        Instant desde = fechaDesde == null ? null : fechaDesde.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant hastaExclusiva = fechaHasta == null
                ? null
                : fechaHasta.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        PageRequest paginacion = Paginacion.crear(pagina, tamano);
        PageRequest ordenada = PageRequest.of(
                paginacion.getPageNumber(),
                paginacion.getPageSize(),
                Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id")));
        return ajusteRepository.buscarConFiltros(
                        productoId, fechaVencimiento, desde, hastaExclusiva, usuarioId, tipo, ordenada)
                .map(ajusteMapper::aRespuesta);
    }

    private void validarSolicitud(AjusteSolicitud solicitud, UsuarioAutenticado usuario) {
        if (solicitud == null || usuario == null) {
            throw new ReglaNegocioException("La solicitud y la identidad autenticada son obligatorias.");
        }
        if (solicitud.stockId() == null || solicitud.stockId() <= 0
                || solicitud.tipo() == null
                || solicitud.cantidad() == null
                || solicitud.cantidad().signum() < 0
                || solicitud.cantidad().scale() > 3
                || solicitud.cantidad().precision() - solicitud.cantidad().scale() > 9) {
            throw new ReglaNegocioException("Los datos del ajuste no son válidos.");
        }
        if (solicitud.motivo() == null || solicitud.motivo().isBlank()) {
            throw new ReglaNegocioException("El motivo del ajuste es obligatorio.");
        }
        if (solicitud.tipo() == TipoAjuste.BAJA && solicitud.cantidad().signum() <= 0) {
            throw new ReglaNegocioException("La cantidad de una baja debe ser mayor que cero.");
        }
    }

    private BigDecimal calcularNuevaCantidad(TipoAjuste tipo, BigDecimal cantidad, BigDecimal anterior) {
        if (tipo == TipoAjuste.CORRECCION) {
            return cantidad.setScale(3);
        }
        if (cantidad.compareTo(anterior) > 0) {
            throw new ReglaNegocioException("La cantidad de la baja supera el stock disponible.");
        }
        BigDecimal nueva = anterior.subtract(cantidad).setScale(3);
        if (nueva.signum() < 0) {
            throw new ReglaNegocioException("El stock resultante no puede ser negativo.");
        }
        return nueva;
    }
}
