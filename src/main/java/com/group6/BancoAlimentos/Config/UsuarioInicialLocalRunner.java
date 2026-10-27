package com.group6.BancoAlimentos.Config;

import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.service.ProvisionamientoUsuariosService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class UsuarioInicialLocalRunner implements ApplicationRunner {

    private final PropiedadesUsuarioInicial propiedades;
    private final ProvisionamientoUsuariosService provisionamientoUsuariosService;

    public UsuarioInicialLocalRunner(
            PropiedadesUsuarioInicial propiedades,
            ProvisionamientoUsuariosService provisionamientoUsuariosService) {
        this.propiedades = propiedades;
        this.provisionamientoUsuariosService = provisionamientoUsuariosService;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        validar(RolUsuario.USER_DEPOSITO, propiedades.getDeposito());
        validar(RolUsuario.USER_INSTITUCIONAL, propiedades.getInstitucional());
        validar(RolUsuario.USER_TESORERIA, propiedades.getTesoreria());
        provisionar(RolUsuario.USER_DEPOSITO, propiedades.getDeposito());
        provisionar(RolUsuario.USER_INSTITUCIONAL, propiedades.getInstitucional());
        provisionar(RolUsuario.USER_TESORERIA, propiedades.getTesoreria());
    }

    private void validar(RolUsuario rol, PropiedadesUsuarioInicial.CuentaInicial cuenta) {
        String nombreUsuario = cuenta.getNombreUsuario();
        String clave = cuenta.getClave();
        if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalStateException(
                    "El usuario y la clave de bootstrap son obligatorios para el rol " + rol.nombre() + ".");
        }
        if (clave.length() < 12 || clave.length() > 128) {
            throw new IllegalStateException(
                    "La clave inicial para el rol " + rol.nombre() + " debe tener entre 12 y 128 caracteres.");
        }
        if (rol == RolUsuario.USER_INSTITUCIONAL
                && (cuenta.getInstitucionId() == null || cuenta.getInstitucionId() <= 0)) {
            throw new IllegalStateException(
                    "El ID de institución inicial debe ser positivo para el rol " + rol.nombre() + ".");
        }
    }

    private void provisionar(RolUsuario rol, PropiedadesUsuarioInicial.CuentaInicial cuenta) {
        String nombreUsuario = cuenta.getNombreUsuario();
        String clave = cuenta.getClave();
        provisionamientoUsuariosService.crearUsuarioInicial(nombreUsuario, clave, rol, cuenta.getInstitucionId());
    }
}
