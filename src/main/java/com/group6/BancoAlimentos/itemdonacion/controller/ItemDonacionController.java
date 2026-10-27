package com.group6.BancoAlimentos.itemdonacion.controller;

import com.group6.BancoAlimentos.donacion.service.ConsultaDonacionService;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionRespuesta;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/donaciones/{donacionId}/items")
public class ItemDonacionController {

    private final ConsultaDonacionService consultaDonacionService;

    public ItemDonacionController(ConsultaDonacionService consultaDonacionService) {
        this.consultaDonacionService = consultaDonacionService;
    }

    @GetMapping
    public List<ItemDonacionRespuesta> listar(@PathVariable Long donacionId) {
        return consultaDonacionService.listarItems(donacionId);
    }
}
