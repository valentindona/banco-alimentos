package com.group6.BancoAlimentos.notificacion.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.notificacion.modelo.Notificacion;
import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import com.group6.BancoAlimentos.notificacion.repository.NotificacionRepository;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
class NotificacionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private StockRepository stockRepository;

    private Long stockId;

    @BeforeEach
    void prepararNotificaciones() {
        notificacionRepository.deleteAll();
        stockId = stockRepository.findAll().get(0).getId();
        notificacionRepository.saveAndFlush(Notificacion.builder()
                .stockId(stockId)
                .tipo(TipoNotificacion.POR_VENCER)
                .audiencia("user_deposito")
                .mensaje("Producto de prueba vence pronto.")
                .build());
        notificacionRepository.saveAndFlush(Notificacion.builder()
                .stockId(stockId)
                .tipo(TipoNotificacion.VENCIDO)
                .audiencia("user_deposito")
                .mensaje("Producto de prueba venció.")
                .build());
    }

    @AfterEach
    void limpiarNotificaciones() {
        notificacionRepository.deleteAll();
    }

    @Test
    void permiteAdepositoFiltrarAvisosYPaginar() throws Exception {
        mockMvc.perform(get("/api/notificaciones")
                        .with(authentication(autenticacion("user_deposito")))
                        .param("tipo", "POR_VENCER")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("POR_VENCER"))
                .andExpect(jsonPath("$.content[0].stockId").value(stockId))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/notificaciones")
                        .with(authentication(autenticacion("user_deposito"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        mockMvc.perform(get("/api/notificaciones")
                        .with(authentication(autenticacion("user_deposito")))
                        .param("tipo", "POR_VENCER")
                        .param("pagina", "0")
                        .param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void deniegaAvisosATesoreriaEInstitucionalYRequiereAutenticacion() throws Exception {
        mockMvc.perform(get("/api/notificaciones")
                        .with(authentication(autenticacion("user_tesoreria"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/notificaciones")
                        .with(authentication(autenticacion("user_institucional"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/notificaciones"))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken autenticacion(String rol) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(41L, "usuario-prueba", List.of(rol), null);
        return new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(() -> "ROLE_" + rol));
    }
}
