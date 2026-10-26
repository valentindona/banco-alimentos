package com.group6.BancoAlimentos.Security;

import static org.assertj.core.api.Assertions.assertThat;

import com.group6.BancoAlimentos.Config.PropiedadesJwt;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class ConfiguracionJwtTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ConfiguracionPrueba.class);

    @Test
    void rechazaConfiguracionSinClaveDeFirma() {
        contextRunner.run(context -> assertThat(context).hasFailed());
    }

    @Test
    void rechazaClaveDeFirmaMenorA256Bits() {
        contextRunner
                .withPropertyValues("seguridad.jwt.secreto=demasiado-corta")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void aceptaClaveDeFirmaValida() {
        contextRunner
                .withPropertyValues("seguridad.jwt.secreto=clave-de-prueba-de-al-menos-256-bits")
                .run(context -> assertThat(context).hasNotFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(PropiedadesJwt.class)
    static class ConfiguracionPrueba {
    }
}
