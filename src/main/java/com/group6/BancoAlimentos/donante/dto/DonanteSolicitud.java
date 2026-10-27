package com.group6.BancoAlimentos.donante.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DonanteSolicitud(
        @NotBlank @Size(max = 150) String nombre,
        @Email @Size(max = 254) String correo,
        @Size(max = 40) String telefono) {}
