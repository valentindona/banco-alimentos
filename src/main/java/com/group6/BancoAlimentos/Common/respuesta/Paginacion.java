package com.group6.BancoAlimentos.Common.respuesta;

import org.springframework.data.domain.PageRequest;

public final class Paginacion {

    public static final int TAMANO_MAXIMO = 100;

    private Paginacion() {
    }

    public static PageRequest crear(int pagina, int tamano) {
        int numeroPagina = Math.max(0, pagina);
        int tamanoPagina = Math.min(Math.max(1, tamano), TAMANO_MAXIMO);
        return PageRequest.of(numeroPagina, tamanoPagina);
    }
}
