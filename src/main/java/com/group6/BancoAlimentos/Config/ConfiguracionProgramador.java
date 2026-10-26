package com.group6.BancoAlimentos.Config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionProgramador {

    @Bean
    @org.springframework.beans.factory.annotation.Qualifier("relojNegocio")
    Clock relojNegocio(@Value("${programador.vencimientos.zona:America/Argentina/Buenos_Aires}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}
