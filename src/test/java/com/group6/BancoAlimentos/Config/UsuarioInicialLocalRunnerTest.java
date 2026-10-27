package com.group6.BancoAlimentos.Config;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.service.ProvisionamientoUsuariosService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.DefaultApplicationArguments;

class UsuarioInicialLocalRunnerTest {

    @Test
    void provisionaLosTresRolesConLasCredencialesConfiguradas() {
        PropiedadesUsuarioInicial propiedades = new PropiedadesUsuarioInicial();
        propiedades.getDeposito().setNombreUsuario("deposito");
        propiedades.getDeposito().setClave("deposito-local-26");
        propiedades.getInstitucional().setNombreUsuario("institucional");
        propiedades.getInstitucional().setClave("institucional-local-26");
        propiedades.getInstitucional().setInstitucionId(1L);
        propiedades.getTesoreria().setNombreUsuario("tesoreria");
        propiedades.getTesoreria().setClave("tesoreria-local-26");
        ProvisionamientoUsuariosService servicio = Mockito.mock(ProvisionamientoUsuariosService.class);

        new UsuarioInicialLocalRunner(propiedades, servicio).run(new DefaultApplicationArguments());

        verify(servicio).crearUsuarioInicial("deposito", "deposito-local-26", RolUsuario.USER_DEPOSITO, null);
        verify(servicio).crearUsuarioInicial(
                "institucional", "institucional-local-26", RolUsuario.USER_INSTITUCIONAL, 1L);
        verify(servicio).crearUsuarioInicial("tesoreria", "tesoreria-local-26", RolUsuario.USER_TESORERIA, null);
    }

    @Test
    void fallaExplicitamenteSiFaltanCredencialesDeCualquierRol() {
        ProvisionamientoUsuariosService servicio = Mockito.mock(ProvisionamientoUsuariosService.class);
        PropiedadesUsuarioInicial propiedades = credencialesValidas();
        propiedades.getTesoreria().setClave(null);
        UsuarioInicialLocalRunner runner = new UsuarioInicialLocalRunner(propiedades, servicio);

        assertThrows(IllegalStateException.class, () -> runner.run(new DefaultApplicationArguments()));
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazaClavesInicialesDebiles() {
        PropiedadesUsuarioInicial propiedades = credencialesValidas();
        propiedades.getInstitucional().setClave("demasiado");
        ProvisionamientoUsuariosService servicio = Mockito.mock(ProvisionamientoUsuariosService.class);

        assertThrows(IllegalStateException.class,
                () -> new UsuarioInicialLocalRunner(propiedades, servicio).run(new DefaultApplicationArguments()));
        verifyNoInteractions(servicio);
    }

    @Test
    void rechazaInstitucionInicialInvalida() {
        PropiedadesUsuarioInicial propiedades = credencialesValidas();
        propiedades.getInstitucional().setInstitucionId(null);
        ProvisionamientoUsuariosService servicio = Mockito.mock(ProvisionamientoUsuariosService.class);

        assertThrows(IllegalStateException.class,
                () -> new UsuarioInicialLocalRunner(propiedades, servicio).run(new DefaultApplicationArguments()));
        verifyNoInteractions(servicio);
    }

    private PropiedadesUsuarioInicial credencialesValidas() {
        PropiedadesUsuarioInicial propiedades = new PropiedadesUsuarioInicial();
        propiedades.getDeposito().setNombreUsuario("deposito");
        propiedades.getDeposito().setClave("deposito-local-26");
        propiedades.getInstitucional().setNombreUsuario("institucional");
        propiedades.getInstitucional().setClave("institucional-local-26");
        propiedades.getInstitucional().setInstitucionId(1L);
        propiedades.getTesoreria().setNombreUsuario("tesoreria");
        propiedades.getTesoreria().setClave("tesoreria-local-26");
        return propiedades;
    }
}
