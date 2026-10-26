package com.group6.BancoAlimentos.Common.excepcion;

import org.springframework.http.HttpStatus;

public class ReglaNegocioException extends RuntimeException {

    private final String codigo;

    public ReglaNegocioException(String mensaje) {
        this("REGLA_NEGOCIO", mensaje);
    }

    public ReglaNegocioException(String codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    public String getCodigo() {
        return codigo;
    }

    public HttpStatus getEstadoHttp() {
        return HttpStatus.UNPROCESSABLE_ENTITY;
    }
}
