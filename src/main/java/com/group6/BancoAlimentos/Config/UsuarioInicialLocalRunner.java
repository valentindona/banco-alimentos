package com.group6.BancoAlimentos.Config;

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
        String nombreUsuario = propiedades.getNombreUsuario();
        String clave = propiedades.getClave();
        if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null || clave.isBlank()) {
            throw new IllegalStateException(
                    "BOOTSTRAP_DEPOSITO_USUARIO y BOOTSTRAP_DEPOSITO_CLAVE son obligatorias con el perfil local.");
        }
        if (clave.length() < 12 || clave.length() > 128) {
            throw new IllegalStateException("La clave inicial debe tener entre 12 y 128 caracteres.");
        }
        provisionamientoUsuariosService.crearUsuarioDepositoInicial(nombreUsuario, clave);
    }
}
