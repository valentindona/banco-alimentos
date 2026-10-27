package com.group6.BancoAlimentos.donante.dto;

public record DonanteRespuesta(
        Long id,
        String nombre,
        String correo,
        String telefono,
        boolean activo) {}
