package com.group6.BancoAlimentos.Config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "seguridad.usuario-inicial")
public class PropiedadesUsuarioInicial {

    private CuentaInicial deposito = new CuentaInicial();
    private CuentaInicial institucional = new CuentaInicial();
    private CuentaInicial tesoreria = new CuentaInicial();

    @Getter
    @Setter
    public static class CuentaInicial {

        private String nombreUsuario;
        private String clave;
        private Long institucionId;
    }
}
