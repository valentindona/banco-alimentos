package com.group6.BancoAlimentos.usuario.modelo;

public enum RolUsuario {
    USER_DEPOSITO("user_deposito"),
    USER_INSTITUCIONAL("user_institucional"),
    USER_TESORERIA("user_tesoreria");

    private final String nombre;

    RolUsuario(String nombre) {
        this.nombre = nombre;
    }

    public String nombre() {
        return nombre;
    }
}
