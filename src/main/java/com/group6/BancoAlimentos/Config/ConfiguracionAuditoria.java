package com.group6.BancoAlimentos.Config;

import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ConfiguracionAuditoria {

    @Bean
    HibernatePropertiesCustomizer configurarAuditoriaEnvers() {
        return propiedades -> propiedades.put("org.hibernate.envers.store_data_at_delete", true);
    }
}
