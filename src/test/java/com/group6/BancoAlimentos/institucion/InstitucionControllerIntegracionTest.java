package com.group6.BancoAlimentos.institucion;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InstitucionControllerIntegracionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private RemitoRepository remitoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Institucion institucion;
    private Usuario usuario;

    @BeforeEach
    void prepararDatos() {
        jdbcTemplate.update("DELETE FROM ajustes_stock");
        remitoRepository.deleteAll();
        usuarioRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM facturas");
        institucionRepository.deleteAll();

        institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución ABM")
                .domicilio("Calle de prueba 123")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
        usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("institucional-abm")
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_INSTITUCIONAL)
                .institucionId(institucion.getId())
                .activo(true)
                .build());
    }

    @Test
    void institucionalPuedeCrearConsultarActualizarYDarDeBaja() throws Exception {
        var autenticar = authentication(tokenInstitucional());

        mockMvc.perform(post("/api/instituciones")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Comedor nuevo","domicilio":"Calle 2","tipo":"Comedor"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estadoPago").value("AL_DIA"))
                .andExpect(jsonPath("$.activa").value(true));

        mockMvc.perform(get("/api/instituciones/{id}", institucion.getId()).with(autenticar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Institución ABM"));

        mockMvc.perform(put("/api/instituciones/{id}", institucion.getId())
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Institución actualizada","domicilio":"Calle 3","tipo":"Centro"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Institución actualizada"))
                .andExpect(jsonPath("$.tipo").value("Centro"));

        remitoRepository.saveAndFlush(Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio(institucion.getDomicilio())
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .fechaCreacion(Instant.now())
                .creadoPorUsuarioId(usuario.getId())
                .build());

        mockMvc.perform(delete("/api/instituciones/{id}", institucion.getId()).with(autenticar))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(
                institucionRepository.findById(institucion.getId()).orElseThrow().isActiva());
        org.junit.jupiter.api.Assertions.assertEquals(
                1, remitoRepository.findAll().size(), "La baja debe conservar los remitos históricos");
    }

    @Test
    void rechazaSolicitudInvalidaYNombreDuplicado() throws Exception {
        var autenticar = authentication(tokenInstitucional());

        mockMvc.perform(post("/api/instituciones")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":" ","domicilio":"Calle 2","tipo":"Comedor"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/instituciones")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"institución abm","domicilio":"Calle 2","tipo":"Comedor"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void soloElRolInstitucionalPuedeAdministrarInstituciones() throws Exception {
        var deposito = autenticacion("user_deposito", 900001L);
        mockMvc.perform(post("/api/instituciones")
                        .with(authentication(deposito))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"No autorizado","domicilio":"Calle 2","tipo":"Comedor"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/instituciones/{id}", institucion.getId())
                        .with(authentication(deposito))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"No autorizado","domicilio":"Calle 2","tipo":"Comedor"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/instituciones/{id}", institucion.getId())
                        .with(authentication(deposito)))
                .andExpect(status().isForbidden());
    }

    private UsernamePasswordAuthenticationToken tokenInstitucional() {
        return autenticacion("user_institucional", usuario.getId());
    }

    private UsernamePasswordAuthenticationToken autenticacion(String rol, Long usuarioId) {
        UsuarioAutenticado identidad = new UsuarioAutenticado(usuarioId, rol, List.of(rol), null);
        return new UsernamePasswordAuthenticationToken(
                identidad, null, List.of(() -> "ROLE_" + rol));
    }
}
