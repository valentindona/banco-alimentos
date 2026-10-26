package com.group6.BancoAlimentos.Config;

import java.time.Clock;
import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfiguracionGeneral {

    @Bean
    Clock relojSistema() {
        return Clock.systemUTC();
    }

    @Bean
    ModelMapper modelMapper() {
        return new ModelMapper();
    }
}
