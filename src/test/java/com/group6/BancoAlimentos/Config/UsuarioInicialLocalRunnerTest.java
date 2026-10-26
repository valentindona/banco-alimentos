package com.group6.BancoAlimentos.Config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.group6.BancoAlimentos.usuario.service.ProvisionamientoUsuariosService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;

class UsuarioInicialLocalRunnerTest {

    @Test
    void provisionaUsuarioDeDepositoConLasCredencialesConfiguradas() {
        PropiedadesUsuarioInicial propiedades = new PropiedadesUsuarioInicial();
        propiedades.setNombreUsuario("deposito-inicial");
        propiedades.setClave("clave-local-con-12-caracteres");
        ProvisionamientoUsuariosService servicio = Mockito.mock(ProvisionamientoUsuariosService.class);

        new UsuarioInicialLocalRunner(propiedades, servicio).run(new DefaultApplicationArguments());

        verify(servicio).crearUsuarioDepositoInicial("deposito-inicial", "clave-local-con-12-caracteres");
    }

    @Test
    void fallaExplicitamenteSiFaltanCredencialesDeBootstrap() {
        ProvisionamientoUsuariosService servicio = Mockito.mock(ProvisionamientoUsuariosService.class);
        UsuarioInicialLocalRunner runner = new UsuarioInicialLocalRunner(new PropiedadesUsuarioInicial(), servicio);

        assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazaClavesInicialesDebiles() {
        PropiedadesUsuarioInicial propiedades = new PropiedadesUsuarioInicial();
        propiedades.setNombreUsuario("deposito-inicial");
        propiedades.setClave("demasiado");
        ProvisionamientoUsuariosService servicio = Mockito.mock(ProvisionamientoUsuariosService.class);

        assertThrows(IllegalStateException.class,
                () -> new UsuarioInicialLocalRunner(propiedades, servicio).run(new DefaultApplicationArguments()));
        verifyNoInteractions(servicio);
    }
}
