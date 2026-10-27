package com.group6.BancoAlimentos.ajuste.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AjustePermisosIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StockRepository stockRepository;

    private Stock stock;

    @BeforeEach
    void prepararStock() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Producto permisos " + UUID.randomUUID())
                .unidadMedida("unidad")
                .build());
        stock = stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(LocalDate.now().plusDays(20))
                .cantidadDisponible(new BigDecimal("5.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
    }

    @Test
    void soloDepositoPuedeConsultarYRegistrarSinCambiosAnteDenegacion() throws Exception {
        BigDecimal stockAntes = stock.getCantidadDisponible();
        int ajustesAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ajustes_stock", Integer.class);
        String cuerpo = """
                {"stockId":%d,"tipo":"BAJA","cantidad":1,"motivo":"Intento no autorizado"}
                """.formatted(stock.getId());

        for (String rol : List.of("user_institucional", "user_tesoreria")) {
            var autenticar = autenticar(rol, rol.equals("user_institucional") ? 900002L : 900003L);
            mockMvc.perform(get("/api/ajustes").with(autenticar)).andExpect(status().isForbidden());
            mockMvc.perform(post("/api/ajustes")
                            .with(autenticar)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(cuerpo))
                    .andExpect(status().isForbidden());
        }

        mockMvc.perform(get("/api/ajustes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isUnauthorized());

        assertEquals(stockAntes, stockRepository.findById(stock.getId()).orElseThrow().getCantidadDisponible());
        assertEquals(ajustesAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ajustes_stock", Integer.class));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor autenticar(String rol, long usuarioId) {
        var principal = new UsuarioAutenticado(usuarioId, rol, List.of(rol), null);
        var token = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
        return authentication(token);
    }
}
