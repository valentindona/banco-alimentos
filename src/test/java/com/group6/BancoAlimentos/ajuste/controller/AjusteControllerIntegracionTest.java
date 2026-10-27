package com.group6.BancoAlimentos.ajuste.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AjusteControllerIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private RemitoRepository remitoRepository;

    @Autowired
    private DetalleRemitoRepository detalleRemitoRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    private Producto producto;
    private Usuario usuario;
    private Institucion institucion;

    @BeforeEach
    void prepararDatos() {
        String sufijo = UUID.randomUUID().toString();
        producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Producto ajuste " + sufijo)
                .unidadMedida("unidad")
                .build());
        usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("deposito-ajuste-" + sufijo)
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
        institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución ajuste " + sufijo)
                .domicilio("Dirección de prueba")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
    }

    @AfterEach
    void limpiarDatos() {
        jdbcTemplate.update(
                "DELETE FROM ajustes_stock WHERE producto_id = ? OR usuario_id = ?", producto.getId(), usuario.getId());
        jdbcTemplate.update("""
                DELETE FROM detalles_remito
                WHERE remito_id IN (SELECT id FROM remitos WHERE institucion_id = ?)
                   OR stock_id IN (SELECT id FROM stock WHERE producto_id = ?)
                """, institucion.getId(), producto.getId());
        jdbcTemplate.update("DELETE FROM remitos WHERE institucion_id = ?", institucion.getId());
        jdbcTemplate.update("DELETE FROM stock WHERE producto_id = ?", producto.getId());
        jdbcTemplate.update("DELETE FROM usuarios WHERE id = ?", usuario.getId());
        jdbcTemplate.update("DELETE FROM instituciones WHERE id = ?", institucion.getId());
        jdbcTemplate.update("DELETE FROM instituciones WHERE nombre = 'Comedor nuevo'");
        jdbcTemplate.update("DELETE FROM productos WHERE id = ?", producto.getId());
    }

    @Test
    void registraBajaYCorreccionConSnapshotsYConsultaConFiltros() throws Exception {
        var autenticar = autenticacion("user_deposito", usuario.getId());
        Stock stock = crearStock("10.000", LocalDate.now().plusDays(30), EstadoStock.DISPONIBLE);

        mockMvc.perform(post("/api/ajustes")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"BAJA","cantidad":2.500,"motivo":"  Producto dañado "}
                                """.formatted(stock.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockId").value(stock.getId()))
                .andExpect(jsonPath("$.productoId").value(producto.getId()))
                .andExpect(jsonPath("$.usuarioId").value(usuario.getId()))
                .andExpect(jsonPath("$.cantidadAnterior").value(10.0))
                .andExpect(jsonPath("$.cantidadNueva").value(7.5))
                .andExpect(jsonPath("$.motivo").value("Producto dañado"));

        mockMvc.perform(post("/api/ajustes")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"CORRECCION","cantidad":0,"motivo":"Recuento físico"}
                                """.formatted(stock.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadNueva").value(0));

        mockMvc.perform(get("/api/ajustes")
                        .with(autenticar)
                        .param("productoId", producto.getId().toString())
                        .param("fechaVencimiento", stock.getFechaVencimiento().toString())
                        .param("fechaDesde", LocalDate.now().toString())
                        .param("fechaHasta", LocalDate.now().toString())
                        .param("usuarioId", usuario.getId().toString())
                        .param("tipo", TipoAjuste.BAJA.name())
                        .param("pagina", "0")
                        .param("tamano", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].tipo").value("BAJA"));

        mockMvc.perform(get("/api/ajustes")
                        .with(autenticar)
                        .param("fechaDesde", "2026-10-28")
                        .param("fechaHasta", "2026-10-27"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/ajustes")
                        .with(autenticar)
                        .param("productoId", "999999")
                        .param("tamano", "500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(0))
                .andExpect(jsonPath("$.tamano").value(100));
    }

    @Test
    void editaYAEliminaAjusteRevirtiendoElStockSoloSinCambiosPosteriores() throws Exception {
        var autenticar = autenticacion("user_deposito", usuario.getId());
        Stock stock = crearStock("10.000", LocalDate.now().plusDays(30), EstadoStock.DISPONIBLE);

        var creado = mockMvc.perform(post("/api/ajustes")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"BAJA","cantidad":2.000,"motivo":"Carga inicial"}
                                """.formatted(stock.getId())))
                .andExpect(status().isCreated())
                .andReturn();
        long ajusteId = Long.parseLong(
                creado.getResponse().getContentAsString().replaceFirst("(?s).*\"id\":(\\d+).*", "$1"));
        assertThatStock(stock.getId(), "8.000");

        mockMvc.perform(put("/api/ajustes/{id}", ajusteId)
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"BAJA","cantidad":3.000,"motivo":"Cantidad corregida"}
                                """.formatted(stock.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadAnterior").value(10.0))
                .andExpect(jsonPath("$.cantidadNueva").value(7.0))
                .andExpect(jsonPath("$.motivo").value("Cantidad corregida"));
        assertThatStock(stock.getId(), "7.000");

        mockMvc.perform(delete("/api/ajustes/{id}", ajusteId).with(autenticar))
                .andExpect(status().isNoContent());
        assertThatStock(stock.getId(), "10.000");
    }

    @Test
    void rechazaCantidadesInvalidasSinCambiarStockNiHistorial() throws Exception {
        var autenticar = autenticacion("user_deposito", usuario.getId());
        Stock stock = crearStock("5.000", LocalDate.now().plusDays(30), EstadoStock.DISPONIBLE);
        int eventosAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ajustes_stock", Integer.class);

        mockMvc.perform(post("/api/ajustes")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"BAJA","cantidad":5.001,"motivo":"No alcanza"}
                                """.formatted(stock.getId())))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/ajustes")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"BAJA","cantidad":1,"motivo":"   "}
                                """.formatted(stock.getId())))
                .andExpect(status().isBadRequest());

        assertThatStock(stock.getId(), "5.000");
        org.junit.jupiter.api.Assertions.assertEquals(
                eventosAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ajustes_stock", Integer.class));
    }

    @Test
    void permiteBajaDeLoteVencidoSinCambiarSuEstado() throws Exception {
        var autenticar = autenticacion("user_deposito", usuario.getId());
        Stock stock = crearStock("3.000", LocalDate.now().minusDays(1), EstadoStock.VENCIDO);

        mockMvc.perform(post("/api/ajustes")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"BAJA","cantidad":1.000,"motivo":"Producto vencido"}
                                """.formatted(stock.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cantidadNueva").value(2.0));

        Stock actualizado = stockRepository.findById(stock.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertEquals(EstadoStock.VENCIDO, actualizado.getEstado());
        org.junit.jupiter.api.Assertions.assertEquals(new BigDecimal("2.000"), actualizado.getCantidadDisponible());
    }

    @Test
    void permiteAjustarLoteDeRemitoBorradorYLaConfirmacionRevalidaSaldo() throws Exception {
        var autenticar = autenticacion("user_deposito", usuario.getId());
        Stock stock = crearStock("5.000", LocalDate.now().plusDays(30), EstadoStock.DISPONIBLE);
        Remito remito = remitoRepository.saveAndFlush(Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio("Calle de prueba")
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .fechaCreacion(Instant.now())
                .creadoPorUsuarioId(usuario.getId())
                .build());
        detalleRemitoRepository.saveAndFlush(DetalleRemito.builder()
                .remitoId(remito.getId())
                .stockId(stock.getId())
                .productoId(stock.getProductoId())
                .fechaVencimiento(stock.getFechaVencimiento())
                .productoNombre(producto.getNombre())
                .cantidad(new BigDecimal("4.000"))
                .build());

        mockMvc.perform(post("/api/ajustes")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":%d,"tipo":"BAJA","cantidad":2.000,"motivo":"Pérdida"}
                                """.formatted(stock.getId())))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/remitos/{id}/confirmacion", remito.getId()).with(autenticar))
                .andExpect(status().isUnprocessableEntity());

        org.junit.jupiter.api.Assertions.assertEquals(
                EstadoRemito.BORRADOR, remitoRepository.findById(remito.getId()).orElseThrow().getEstado());
        assertThatStock(stock.getId(), "3.000");
    }

    @Test
    void requiereAutenticacionYDeniegaRolesNoAutorizados() throws Exception {
        mockMvc.perform(get("/api/ajustes")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/ajustes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":900001,"tipo":"BAJA","cantidad":1,"motivo":"Motivo"}
                                """))
                .andExpect(status().isUnauthorized());

        var institucional = autenticacion("user_institucional", 900002L);
        mockMvc.perform(get("/api/ajustes").with(institucional)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/ajustes")
                        .with(institucional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"stockId":900001,"tipo":"BAJA","cantidad":1,"motivo":"Motivo"}
                                """))
                .andExpect(status().isForbidden());
    }

    private Stock crearStock(String cantidad, LocalDate vencimiento, EstadoStock estado) {
        return stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal(cantidad))
                .estado(estado)
                .build());
    }

    private void assertThatStock(Long stockId, String cantidadEsperada) {
        org.junit.jupiter.api.Assertions.assertEquals(
                new BigDecimal(cantidadEsperada),
                stockRepository.findById(stockId).orElseThrow().getCantidadDisponible());
    }

    private RequestPostProcessor autenticacion(String rol, long usuarioId) {
        UsuarioAutenticado principal = new UsuarioAutenticado(usuarioId, rol, List.of(rol), null);
        return authentication(UsernamePasswordAuthenticationToken.authenticated(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
