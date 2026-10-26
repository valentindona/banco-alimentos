package com.group6.BancoAlimentos.notificacion.controller;

import com.group6.BancoAlimentos.notificacion.dto.NotificacionRespuesta;
import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import com.group6.BancoAlimentos.notificacion.service.ConsultaNotificacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notificaciones")
public class NotificacionController {

    private final ConsultaNotificacionService consultaNotificacionService;

    public NotificacionController(ConsultaNotificacionService consultaNotificacionService) {
        this.consultaNotificacionService = consultaNotificacionService;
    }

    @GetMapping
    public Page<NotificacionRespuesta> listar(
            @RequestParam(required = false) TipoNotificacion tipo,
            @RequestParam(name = "pagina", required = false) Integer pagina,
            @RequestParam(name = "tamano", required = false) Integer tamano,
            @RequestParam(name = "page", required = false) Integer paginaAnterior,
            @RequestParam(name = "size", required = false) Integer tamanoAnterior,
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC)
                    Pageable pageable) {
        int numeroPagina = pagina != null
                ? pagina
                : paginaAnterior != null ? paginaAnterior : pageable.getPageNumber();
        int tamanoPagina = tamano != null
                ? tamano
                : tamanoAnterior != null ? tamanoAnterior : pageable.getPageSize();
        Pageable paginaSolicitada = PageRequest.of(
                Math.max(0, numeroPagina),
                Math.max(1, tamanoPagina),
                pageable.getSort());
        return consultaNotificacionService.listar(tipo, paginaSolicitada);
    }
}
