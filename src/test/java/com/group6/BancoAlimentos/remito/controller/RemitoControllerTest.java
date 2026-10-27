package com.group6.BancoAlimentos.remito.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RemitoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private RemitoRepository remitoRepository;

    @Autowired
    private DetalleRemitoRepository detalleRemitoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Institucion institucion;
    private Stock stock;
    private Usuario usuarioDeposito;

    @BeforeEach
    void prepararDatos() {
        detalleRemitoRepository.deleteAll();
        remitoRepository.deleteAll();
        stockRepository.deleteAll();
        usuarioRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM facturas");
        institucionRepository.deleteAll();
        productoRepository.deleteAll();
        institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución de prueba")
                .domicilio("Calle de prueba 123")
                .estadoPago(EstadoPago.EN_DEUDA)
                .activa(true)
                .build());
        usuarioDeposito = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("deposito-controller")
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Arroz")
                .unidadMedida("kg")
                .activo(true)
                .build());
        stock = stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .cantidadDisponible(new BigDecimal("10.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
    }

    @AfterEach
    void limpiarDatos() {
        detalleRemitoRepository.deleteAll();
        remitoRepository.deleteAll();
        stockRepository.deleteAll();
        usuarioRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM facturas");
        institucionRepository.deleteAll();
        productoRepository.deleteAll();
    }

    @Test
    void creaBorradorAgregaExistenciaElegidaYConfirmaDescuento() throws Exception {
        var autenticar = autenticacion("user_deposito");
        var crear = mockMvc.perform(post("/api/remitos")
                        .with(authentication(autenticar))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"institucionId":%d,"fechaEntrega":"%s"}
                                """.formatted(institucion.getId(), LocalDate.now().plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("BORRADOR"))
                .andExpect(jsonPath("$.institucionNombre").value("Institución de prueba"))
                .andReturn();
        long remitoId = objectMapper.readTree(crear.getResponse().getContentAsByteArray()).get("id").asLong();

        mockMvc.perform(post("/api/remitos/{id}/detalles", remitoId)
                        .with(authentication(autenticar))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"cantidad":2.5}
                                """.formatted(stock.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockId").value(stock.getId()))
                .andExpect(jsonPath("$.fechaVencimiento").value(stock.getFechaVencimiento().toString()));

        mockMvc.perform(post("/api/remitos/{id}/confirmacion", remitoId)
                        .with(authentication(autenticar)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CONFIRMADO"));
        assertEquals(new BigDecimal("7.500"), stockRepository.findById(stock.getId()).orElseThrow()
                .getCantidadDisponible());
    }

    @Test
    void devuelve422ConFormatoEstandarParaStockVencidoYValidaDto() throws Exception {
        var autenticar = autenticacion("user_deposito");
        var crear = mockMvc.perform(post("/api/remitos")
                        .with(authentication(autenticar))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"institucionId":%d,"fechaEntrega":"%s"}
                                """.formatted(institucion.getId(), LocalDate.now().plusDays(1))))
                .andExpect(status().isCreated())
                .andReturn();

        long remitoId = objectMapper.readTree(crear.getResponse().getContentAsByteArray()).get("id").asLong();
        stock.setFechaVencimiento(LocalDate.now());
        stockRepository.saveAndFlush(stock);
        mockMvc.perform(post("/api/remitos/{id}/detalles", remitoId)
                        .with(authentication(autenticar))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"cantidad":1}
                                """.formatted(stock.getId())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("REGLA_NEGOCIO"));

        mockMvc.perform(post("/api/remitos")
                        .with(authentication(autenticar))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"institucionId":null,"fechaEntrega":null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erroresCampo").isArray());
    }

    @Test
    void restringeEscriturasADepositoYRequiereAutenticacion() throws Exception {
        String solicitud = """
                {"institucionId":%d,"fechaEntrega":"%s"}
                """.formatted(institucion.getId(), LocalDate.now().plusDays(1));
        mockMvc.perform(post("/api/remitos")
                        .with(authentication(autenticacion("user_tesoreria")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/remitos")
                        .with(authentication(autenticacion("user_institucional")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/remitos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud))
                .andExpect(status().isUnauthorized());
    }

    private UsernamePasswordAuthenticationToken autenticacion(String rol) {
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                rol.equals("user_deposito") ? usuarioDeposito.getId() : 41L,
                "deposito",
                List.of(rol),
                null);
        return new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(() -> "ROLE_" + rol));
    }
}
