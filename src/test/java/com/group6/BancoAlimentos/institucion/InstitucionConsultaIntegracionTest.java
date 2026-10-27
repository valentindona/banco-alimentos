package com.group6.BancoAlimentos.institucion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
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
class InstitucionConsultaIntegracionTest {

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

    private Institucion institucionCoincidente;
    private Institucion institucionDistractora;

    @BeforeEach
    void prepararDatos() {
        jdbcTemplate.update("DELETE FROM ajustes_stock");
        remitoRepository.deleteAll();
        usuarioRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM facturas");
        institucionRepository.deleteAll();

        institucionCoincidente = guardar("Comedor Norte", "Comedor", EstadoPago.EN_DEUDA, true);
        institucionDistractora = guardar("Comedor Sur", "Comedor", EstadoPago.AL_DIA, true);
        guardar("Centro Norte", "Centro", EstadoPago.EN_DEUDA, true);
        guardar("Comedor Norte inactivo", "Comedor", EstadoPago.EN_DEUDA, false);
    }

    @Test
    void losTresRolesPuedenConsultarListadoYDetalle() throws Exception {
        for (String rol : List.of("user_deposito", "user_tesoreria", "user_institucional")) {
            var autenticar = authentication(autenticacion(rol));

            mockMvc.perform(get("/api/instituciones").with(autenticar))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contenido.length()").value(3))
                    .andExpect(jsonPath("$.tamano").value(20));

            mockMvc.perform(get("/api/instituciones/{id}", institucionCoincidente.getId())
                            .with(autenticar))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(institucionCoincidente.getId()));
        }
    }

    @Test
    void combinaFiltrosYDevuelvePaginaVaciaCuandoNoHayCoincidencias() throws Exception {
        mockMvc.perform(get("/api/instituciones")
                        .param("nombre", "  norte  ")
                        .param("tipo", "  Comedor  ")
                        .param("estadoPago", "EN_DEUDA")
                        .with(authentication(autenticacion("user_deposito"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(institucionCoincidente.getId()));

        mockMvc.perform(get("/api/instituciones")
                        .param("nombre", "no existe")
                        .with(authentication(autenticacion("user_tesoreria"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido").isEmpty())
                .andExpect(jsonPath("$.totalElementos").value(0));
    }

    @Test
    void limitaTamanoDePaginaACien() throws Exception {
        mockMvc.perform(get("/api/instituciones")
                        .param("pagina", "-1")
                        .param("tamano", "150")
                        .with(authentication(autenticacion("user_deposito"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(100));
    }

    @Test
    void depositoPuedeLeerPeroNoModificarInstituciones() throws Exception {
        var autenticar = authentication(autenticacion("user_deposito"));

        mockMvc.perform(post("/api/instituciones")
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"No autorizada","domicilio":"Calle 5","tipo":"Comedor"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/instituciones/{id}", institucionCoincidente.getId())
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Nombre alterado","domicilio":"Calle 5","tipo":"Comedor"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete("/api/instituciones/{id}", institucionCoincidente.getId())
                        .with(autenticar))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch("/api/instituciones/{id}/estado-pago", institucionCoincidente.getId())
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoPago":"PARTICULAR"}
                                """))
                .andExpect(status().isForbidden());

        assertEquals(4, institucionRepository.count());
        assertEquals("Comedor Norte",
                institucionRepository.findById(institucionCoincidente.getId()).orElseThrow().getNombre());
        assertEquals(EstadoPago.EN_DEUDA,
                institucionRepository.findById(institucionCoincidente.getId()).orElseThrow().getEstadoPago());
        assertEquals("Comedor Sur",
                institucionRepository.findById(institucionDistractora.getId()).orElseThrow().getNombre());
    }

    private Institucion guardar(String nombre, String tipo, EstadoPago estadoPago, boolean activa) {
        return institucionRepository.saveAndFlush(Institucion.builder()
                .nombre(nombre)
                .domicilio("Calle de prueba 123")
                .tipo(tipo)
                .estadoPago(estadoPago)
                .activa(activa)
                .build());
    }

    private UsernamePasswordAuthenticationToken autenticacion(String rol) {
        UsuarioAutenticado identidad = new UsuarioAutenticado(940001L, rol, List.of(rol), null);
        return new UsernamePasswordAuthenticationToken(
                identidad, null, List.of(() -> "ROLE_" + rol));
    }

}
