package com.group6.BancoAlimentos.remito.service;

import com.group6.BancoAlimentos.Common.respuesta.Paginacion;
import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.detalleremito.dto.DetalleRemitoRespuesta;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.remito.dto.RemitoRespuesta;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaRemitoService {

    private final RemitoRepository remitoRepository;
    private final DetalleRemitoRepository detalleRemitoRepository;

    public ConsultaRemitoService(
            RemitoRepository remitoRepository, DetalleRemitoRepository detalleRemitoRepository) {
        this.remitoRepository = remitoRepository;
        this.detalleRemitoRepository = detalleRemitoRepository;
    }

    @Transactional(readOnly = true)
    public Page<RemitoRespuesta> listar(
            Long institucionId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoRemito estado,
            int pagina,
            int tamano) {
        return listar(institucionId, fechaDesde, fechaHasta, estado, pagina, tamano, null);
    }

    @Transactional(readOnly = true)
    public Page<RemitoRespuesta> listar(
            Long institucionId,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            EstadoRemito estado,
            int pagina,
            int tamano,
            UsuarioAutenticado usuario) {
        Long institucionFiltro = institucionId;
        if (usuario != null && usuario.roles().contains("user_institucional")) {
            if (usuario.institucionId() == null) {
                throw new AccessDeniedException("El usuario institucional no tiene una institución asignada.");
            }
            institucionFiltro = usuario.institucionId();
        }

        return remitoRepository.buscarConFiltros(
                        institucionFiltro,
                        fechaDesde,
                        fechaHasta,
                        estado,
                        Paginacion.crear(pagina, tamano))
                .map(this::aRespuesta);
    }

    private RemitoRespuesta aRespuesta(Remito remito) {
        List<DetalleRemitoRespuesta> detalles =
                detalleRemitoRepository.findByRemitoIdOrderByStockIdAsc(remito.getId()).stream()
                        .map(this::aRespuesta)
                        .toList();
        return new RemitoRespuesta(
                remito.getId(),
                remito.getInstitucionId(),
                remito.getInstitucionNombre(),
                remito.getInstitucionDomicilio(),
                remito.getFechaEntrega(),
                remito.getEstado(),
                detalles);
    }

    private DetalleRemitoRespuesta aRespuesta(DetalleRemito detalle) {
        return new DetalleRemitoRespuesta(
                detalle.getId(),
                detalle.getProductoId(),
                detalle.getProductoNombre(),
                detalle.getStockId(),
                detalle.getFechaVencimiento(),
                detalle.getCantidad());
    }
}
