package com.group6.BancoAlimentos.notificacion.controller;

import com.group6.BancoAlimentos.notificacion.dto.NotificacionRespuesta;
import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import com.group6.BancoAlimentos.notificacion.service.ConsultaNotificacionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return consultaNotificacionService.listar(tipo, pageable);
    }
}
