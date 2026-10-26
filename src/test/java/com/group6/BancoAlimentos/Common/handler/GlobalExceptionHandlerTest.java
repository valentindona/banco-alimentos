package com.group6.BancoAlimentos.Common.handler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void configurarControladorDePrueba() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorErrores())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void devuelveFormatoEstandarizadoParaErroresDeValidacion() throws Exception {
        mockMvc.perform(post("/pruebas/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"campo\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400))
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.erroresCampo[0].campo").value("campo"));
    }

    @Test
    void convierteReglasDeNegocioEnHttp422() throws Exception {
        mockMvc.perform(get("/pruebas/regla"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.estado").value(422))
                .andExpect(jsonPath("$.codigo").value("STOCK_VENCIDO"))
                .andExpect(jsonPath("$.mensaje").value("La existencia está vencida."));
    }

    @Test
    void ocultaDetallesInternosDeErroresInesperados() throws Exception {
        mockMvc.perform(get("/pruebas/interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.estado").value(500))
                .andExpect(jsonPath("$.codigo").value("ERROR_INTERNO"))
                .andExpect(jsonPath("$.mensaje").value("Ocurrió un error inesperado."));
    }

    @RestController
    static class ControladorErrores {

        @PostMapping("/pruebas/validacion")
        void validar(@Valid @RequestBody SolicitudPrueba solicitud) {
        }

        @GetMapping("/pruebas/regla")
        void regla() {
            throw new ReglaNegocioException("STOCK_VENCIDO", "La existencia está vencida.");
        }

        @GetMapping("/pruebas/interno")
        void interno() {
            throw new IllegalStateException("detalle confidencial");
        }
    }

    record SolicitudPrueba(@NotBlank String campo) {
    }
}
