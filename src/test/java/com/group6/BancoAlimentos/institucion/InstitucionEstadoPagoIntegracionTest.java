package com.group6.BancoAlimentos.institucion;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
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
class InstitucionEstadoPagoIntegracionTest {

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
    private Usuario usuarioDeposito;

    @BeforeEach
    void prepararDatos() {
        remitoRepository.deleteAll();
        usuarioRepository.deleteAll();
        jdbcTemplate.update("DELETE FROM facturas");
        institucionRepository.deleteAll();

        institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución estado de pago")
                .domicilio("Calle de prueba 123")
                .tipo("Comedor")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
        usuarioDeposito = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("deposito-estado-pago")
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
    }

    @Test
    void tesoreriaActualizaEstadoPagoYRepetirElCambioEsIdempotente() throws Exception {
        var autenticar = authentication(autenticacion("user_tesoreria", 930001L));

        mockMvc.perform(patch("/api/instituciones/{id}/estado-pago", institucion.getId())
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoPago":"EN_DEUDA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("EN_DEUDA"));

        mockMvc.perform(patch("/api/instituciones/{id}/estado-pago", institucion.getId())
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoPago":"EN_DEUDA"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("EN_DEUDA"));

        mockMvc.perform(get("/api/instituciones/{id}", institucion.getId()).with(autenticar))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoPago").value("EN_DEUDA"));
        org.junit.jupiter.api.Assertions.assertEquals(
                EstadoPago.EN_DEUDA,
                institucionRepository.findById(institucion.getId()).orElseThrow().getEstadoPago());
    }

    @Test
    void rechazaEstadoPagoInvalidoOSinValor() throws Exception {
        var autenticar = authentication(autenticacion("user_tesoreria", 930002L));

        mockMvc.perform(patch("/api/instituciones/{id}/estado-pago", institucion.getId())
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoPago":"NO_VALIDO"}
                                """))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/instituciones/{id}/estado-pago", institucion.getId())
                        .with(autenticar)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoPago":null}
                                """))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(
                EstadoPago.AL_DIA,
                institucionRepository.findById(institucion.getId()).orElseThrow().getEstadoPago());
    }

    @Test
    void soloTesoreriaPuedeCambiarElEstadoDePago() throws Exception {
        for (String rol : List.of("user_institucional", "user_deposito")) {
            mockMvc.perform(patch("/api/instituciones/{id}/estado-pago", institucion.getId())
                            .with(authentication(autenticacion(rol, 930003L)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"estadoPago":"PARTICULAR"}
                                    """))
                    .andExpect(status().isForbidden());
        }

        org.junit.jupiter.api.Assertions.assertEquals(
                EstadoPago.AL_DIA,
                institucionRepository.findById(institucion.getId()).orElseThrow().getEstadoPago());
    }

    @Test
    void permiteCrearRemitoParaInstitucionEnDeuda() throws Exception {
        mockMvc.perform(patch("/api/instituciones/{id}/estado-pago", institucion.getId())
                        .with(authentication(autenticacion("user_tesoreria", 930004L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estadoPago":"EN_DEUDA"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/remitos")
                        .with(authentication(autenticacion("user_deposito", usuarioDeposito.getId())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"institucionId":%d,"fechaEntrega":"%s"}
                                """.formatted(institucion.getId(), LocalDate.now().plusDays(1))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.institucionId").value(institucion.getId()))
                .andExpect(jsonPath("$.estado").value("BORRADOR"));
    }

    private UsernamePasswordAuthenticationToken autenticacion(String rol, Long usuarioId) {
        UsuarioAutenticado identidad = new UsuarioAutenticado(usuarioId, rol, List.of(rol), null);
        return new UsernamePasswordAuthenticationToken(
                identidad, null, List.of(() -> "ROLE_" + rol));
    }
}
