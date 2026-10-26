package com.group6.BancoAlimentos.Config;

import java.time.Clock;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ConfiguracionGeneral {

    @Bean
    @Primary
    Clock relojSistema() {
        return Clock.systemUTC();
    }

    @Bean
    ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
