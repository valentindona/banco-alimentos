package com.group6.BancoAlimentos.Seguridad;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    private String iniciarSesion(String nombreUsuario) throws Exception {
        MvcResult resultado = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsBytes(new LoginSolicitud(nombreUsuario, "clave-de-prueba"))))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(resultado.getResponse().getContentAsByteArray()).get("token").asText();
    }
}
