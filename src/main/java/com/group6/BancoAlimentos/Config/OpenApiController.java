package com.group6.BancoAlimentos.Config;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OpenApiController {

    @GetMapping(value = "/openapi.yaml", produces = "application/yaml")
    public Resource obtenerContrato() {
        return new ClassPathResource("openapi.yaml");
    }
}
