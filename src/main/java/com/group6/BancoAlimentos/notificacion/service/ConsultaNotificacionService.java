package com.group6.BancoAlimentos.notificacion.service;

import com.group6.BancoAlimentos.notificacion.dto.NotificacionRespuesta;
import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import com.group6.BancoAlimentos.notificacion.repository.NotificacionRepository;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultaNotificacionService {

    private static final int TAMANO_MAXIMO_PAGINA = 100;
    private final NotificacionRepository notificacionRepository;

    public ConsultaNotificacionService(NotificacionRepository notificacionRepository) {
        this.notificacionRepository = notificacionRepository;
    }

    @Transactional(readOnly = true)
    public Page<NotificacionRespuesta> listar(TipoNotificacion tipo, Pageable pageable) {
        Pageable paginaSegura = normalizarPagina(pageable);
        return (tipo == null
                ? notificacionRepository.findAll(paginaSegura)
                : notificacionRepository.findAllByTipo(tipo, paginaSegura))
                .map(notificacion -> new NotificacionRespuesta(
                        notificacion.getId(),
                        notificacion.getStockId(),
                        notificacion.getTipo(),
                        notificacion.getAudiencia(),
                        notificacion.getFechaCreacion(),
                        notificacion.getMensaje()));
    }

    private Pageable normalizarPagina(Pageable pageable) {
        Objects.requireNonNull(pageable, "La página es obligatoria.");
        int tamano = Math.min(pageable.getPageSize(), TAMANO_MAXIMO_PAGINA);
        Sort sort = pageable.getSort().isSorted()
                ? pageable.getSort()
                : Sort.by(Sort.Direction.DESC, "fechaCreacion");
        return PageRequest.of(pageable.getPageNumber(), tamano, sort);
    }
}
