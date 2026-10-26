package com.group6.BancoAlimentos.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "seguridad.usuario-inicial")
public class PropiedadesUsuarioInicial {

    private String nombreUsuario;
    private String clave;
}
