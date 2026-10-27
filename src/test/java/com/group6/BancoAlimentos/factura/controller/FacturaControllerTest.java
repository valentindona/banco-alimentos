package com.group6.BancoAlimentos.factura.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FacturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void permiteConsultaSoloADeposito() throws Exception {
        mockMvc.perform(get("/api/facturas")
                        .with(authentication(autenticacion("user_deposito"))))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/facturas")
                        .with(authentication(autenticacion("user_tesoreria"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/facturas")
                        .with(authentication(autenticacion("user_institucional"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void noExponeOperacionesDeCreacionNiModificacion() throws Exception {
        var deposito = authentication(autenticacion("user_deposito"));
        mockMvc.perform(post("/api/facturas").with(deposito)).andExpect(status().isMethodNotAllowed());
        mockMvc.perform(put("/api/facturas/1").with(deposito)).andExpect(status().isNotFound());
        mockMvc.perform(post("/api/facturas/generacion")
                        .with(authentication(autenticacion("user_tesoreria")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"donacionId\":1}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/facturas/generacion")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"donacionId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicaLaEspecificacionOpenApiDeLaAplicacion() throws Exception {
        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/facturas:")))
                .andExpect(content().string(containsString("/auth/yo:")));
    }

    private UsernamePasswordAuthenticationToken autenticacion(String rol) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(41L, "usuario-prueba", List.of(rol), null);
        return new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(() -> "ROLE_" + rol));
    }
}
