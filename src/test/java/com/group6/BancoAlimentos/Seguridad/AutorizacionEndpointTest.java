package com.group6.BancoAlimentos.Seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.usuario.dto.AltaUsuarioSolicitud;
import com.group6.BancoAlimentos.usuario.dto.LoginSolicitud;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.util.EnumMap;
import java.util.Map;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AutorizacionEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final Map<RolUsuario, Usuario> usuarios = new EnumMap<>(RolUsuario.class);

    @BeforeEach
    void crearCuentasPorRol() {
        jdbcTemplate.update("DELETE FROM ajustes_stock");
        usuarioRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM facturas");
        institucionRepository.deleteAll();
        Institucion institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución de prueba")
                .domicilio("Calle de prueba 123")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());

        usuarios.clear();
        for (RolUsuario rol : RolUsuario.values()) {
            Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                    .nombreUsuario(rol.nombre())
                    .claveHash(passwordEncoder.encode("clave-de-prueba"))
                    .rol(rol)
                    .institucionId(rol == RolUsuario.USER_INSTITUCIONAL ? institucion.getId() : null)
                    .activo(true)
                    .build());
            usuarios.put(rol, usuario);
        }
    }

    @Test
    void loginEsPublicoYDevuelveTokenSinDatosSensibles() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginSolicitud("user_deposito", "clave-de-prueba"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginSolicitud("user_deposito", "incorrecta"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void endpointAutenticadoAdmiteCadaRolYProvisionamientoSoloAdmiteDeposito() throws Exception {
        for (Map.Entry<RolUsuario, Usuario> entrada : usuarios.entrySet()) {
            String token = iniciarSesion(entrada.getKey().nombre());
            mockMvc.perform(get("/api/auth/yo").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.nombreUsuario").value(entrada.getKey().nombre()));

            AltaUsuarioSolicitud solicitud = new AltaUsuarioSolicitud(
                    "nuevo-" + entrada.getKey().name().toLowerCase(),
                    "clave-nueva-123",
                    RolUsuario.USER_TESORERIA,
                    null);
            var peticion = post("/api/usuarios")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsBytes(solicitud));

            if (entrada.getKey() == RolUsuario.USER_DEPOSITO) {
                mockMvc.perform(peticion)
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.nombreUsuario").value(solicitud.nombreUsuario()))
                        .andExpect(jsonPath("$.claveHash").doesNotExist());
            } else {
                mockMvc.perform(peticion).andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void requiereAutenticacionParaRutasPrivadas() throws Exception {
        mockMvc.perform(get("/api/auth/yo")).andExpect(status().isUnauthorized());
    }

    @Test
    void soloDepositoPuedeConsultarYModificarLosRecursosDeDonaciones() throws Exception {
        long donantesAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM donantes", Long.class);
        long productosAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM productos", Long.class);
        long donacionesAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM donaciones", Long.class);
        long itemsAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM items_donacion", Long.class);
        long stockAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock", Long.class);
        long facturasAntes = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM facturas", Long.class);
        Map<String, Object> donante = Map.of("nombre", "Donante no autorizado");
        Map<String, Object> producto =
                Map.of("nombre", "Producto no autorizado", "tipoAlimento", "Seco", "unidadMedida", "kg");
        Map<String, Object> item = Map.of(
                "nombreProducto", "Manual",
                "tipoAlimento", "Seco",
                "unidadMedida", "kg",
                "cantidad", 1,
                "fechaVencimiento", "2099-01-01");
        Map<String, Object> donacion = Map.of(
                "donanteId", 1,
                "fecha", "2026-10-26",
                "items", List.of(item));

        for (RolUsuario rol : List.of(RolUsuario.USER_TESORERIA, RolUsuario.USER_INSTITUCIONAL)) {
            String token = iniciarSesion(rol.nombre());
            for (String ruta : List.of(
                    "/api/donantes",
                    "/api/donantes/1",
                    "/api/donaciones",
                    "/api/donaciones/1",
                    "/api/donaciones/1/items",
                    "/api/productos",
                    "/api/stock",
                    "/api/facturas")) {
                mockMvc.perform(get(ruta).header("Authorization", "Bearer " + token))
                        .andExpect(status().isForbidden());
            }

            var autorizacion = "Bearer " + token;
            mockMvc.perform(post("/api/donantes")
                            .header("Authorization", autorizacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(donante)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/donaciones")
                            .header("Authorization", autorizacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(donacion)))
                    .andExpect(status().isForbidden());
            mockMvc.perform(post("/api/productos")
                            .header("Authorization", autorizacion)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsBytes(producto)))
                    .andExpect(status().isForbidden());
            for (String ruta : List.of("/api/donantes/1", "/api/productos/1")) {
                mockMvc.perform(put(ruta)
                                .header("Authorization", autorizacion)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(
                                        ruta.contains("donantes") ? donante : producto)))
                        .andExpect(status().isForbidden());
                mockMvc.perform(delete(ruta).header("Authorization", autorizacion))
                        .andExpect(status().isForbidden());
            }
        }

        org.junit.jupiter.api.Assertions.assertEquals(
                donantesAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM donantes", Long.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                productosAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM productos", Long.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                donacionesAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM donaciones", Long.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                itemsAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM items_donacion", Long.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                stockAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM stock", Long.class));
        org.junit.jupiter.api.Assertions.assertEquals(
                facturasAntes, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM facturas", Long.class));
    }

    private String iniciarSesion(String nombreUsuario) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginSolicitud(nombreUsuario, "clave-de-prueba"))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsByteArray()).get("token").asText();
    }
}
