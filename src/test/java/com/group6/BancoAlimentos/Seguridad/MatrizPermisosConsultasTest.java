package com.group6.BancoAlimentos.Seguridad;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.factura.modelo.Factura;
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
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MatrizPermisosConsultasTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RemitoRepository remitoRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void depositoPuedeConsultarTodosLosListadosDeSuRol() throws Exception {
        var autenticar = authentication(autenticacion("user_deposito", null));
        for (String ruta : List.of(
                "/api/remitos", "/api/productos", "/api/stock", "/api/instituciones", "/api/facturas")) {
            mockMvc.perform(get(ruta).with(autenticar)).andExpect(status().isOk());
        }
    }

    @Test
    void tesoreriaSoloPuedeConsultarInstituciones() throws Exception {
        mockMvc.perform(get("/api/instituciones")
                        .with(authentication(autenticacion("user_tesoreria", null))))
                .andExpect(status().isOk());
        for (String ruta : List.of("/api/remitos", "/api/productos", "/api/stock", "/api/facturas")) {
            mockMvc.perform(get(ruta).with(authentication(autenticacion("user_tesoreria", null))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void institucionalSoloPuedeConsultarSusRemitos() throws Exception {
        var autenticar = autenticacion("user_institucional", 900001L);
        mockMvc.perform(get("/api/remitos")
                        .with(authentication(autenticar))
                        .param("institucionId", "123456"))
                .andExpect(status().isOk());
        for (String ruta : List.of("/api/productos", "/api/stock", "/api/instituciones", "/api/facturas")) {
            mockMvc.perform(get(ruta).with(authentication(autenticacion("user_institucional", 900001L))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @Transactional
    @Rollback
    void aplicaFiltrosYPaginacionEnListadosYLimitaRemitosALaInstitucionAutenticada() throws Exception {
        Institucion institucionPropia = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("US3 Institucion propia")
                .domicilio("Calle US3 1")
                .estadoPago(EstadoPago.EN_DEUDA)
                .activa(true)
                .build());
        Institucion otraInstitucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("US3 Institucion ajena")
                .domicilio("Calle US3 2")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("US3 Arroz integral")
                .unidadMedida("kg")
                .activo(true)
                .build());
        LocalDate vencimiento = LocalDate.now().plusDays(30);
        Stock stock = stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal("14.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
        Usuario deposito = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("us3-deposito-filtros")
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
        LocalDate fechaEntrega = LocalDate.now().plusDays(1);
        Remito remitoPropio = remitoRepository.saveAndFlush(crearRemito(
                institucionPropia, deposito, fechaEntrega));
        Remito remitoAjeno = remitoRepository.saveAndFlush(crearRemito(
                otraInstitucion, deposito, fechaEntrega));
        LocalDate fechaFactura = LocalDate.now();
        Factura factura = Factura.builder()
                .numero("US3-F-1")
                .institucionId(institucionPropia.getId())
                .fecha(fechaFactura)
                .estado("PENDIENTE")
                .importe(new BigDecimal("1250.00"))
                .build();
        entityManager.persist(factura);
        entityManager.flush();

        var autenticarDeposito = authentication(autenticacion("user_deposito", null));
        mockMvc.perform(get("/api/productos")
                        .with(autenticarDeposito)
                        .param("nombre", "US3 Arroz")
                        .param("activo", "true")
                        .param("pagina", "0")
                        .param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(producto.getId()))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(1));
        mockMvc.perform(get("/api/stock")
                        .with(autenticarDeposito)
                        .param("productoId", producto.getId().toString())
                        .param("fechaVencimiento", vencimiento.toString())
                        .param("estado", "DISPONIBLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(stock.getId()))
                .andExpect(jsonPath("$.contenido[0].productoNombre").value(producto.getNombre()));
        mockMvc.perform(get("/api/stock")
                        .with(autenticarDeposito)
                        .param("estado", "NO_ES_UN_ESTADO"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.estado").value(400));
        mockMvc.perform(get("/api/instituciones")
                        .with(autenticarDeposito)
                        .param("nombre", "US3 Institucion propia")
                        .param("estadoPago", "EN_DEUDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(institucionPropia.getId()));
        mockMvc.perform(get("/api/remitos")
                        .with(autenticarDeposito)
                        .param("institucionId", institucionPropia.getId().toString())
                        .param("fechaDesde", fechaEntrega.toString())
                        .param("fechaHasta", fechaEntrega.toString())
                        .param("estado", "BORRADOR")
                        .param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(remitoPropio.getId()));
        mockMvc.perform(get("/api/remitos")
                        .with(authentication(autenticacion(
                                "user_institucional", institucionPropia.getId())))
                        .param("institucionId", otraInstitucion.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].institucionId").value(institucionPropia.getId()));
        mockMvc.perform(get("/api/facturas")
                        .with(autenticarDeposito)
                        .param("institucionId", institucionPropia.getId().toString())
                        .param("fechaDesde", fechaFactura.toString())
                        .param("fechaHasta", fechaFactura.toString())
                        .param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(factura.getId()));

    }

    private Remito crearRemito(Institucion institucion, Usuario usuario, LocalDate fechaEntrega) {
        return Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio(institucion.getDomicilio())
                .fechaEntrega(fechaEntrega)
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(usuario.getId())
                .build();
    }

    private UsernamePasswordAuthenticationToken autenticacion(String rol, Long institucionId) {
        UsuarioAutenticado usuario =
                new UsuarioAutenticado(41L, "usuario-prueba", List.of(rol), institucionId);
        return new UsernamePasswordAuthenticationToken(
                usuario, null, List.of(() -> "ROLE_" + rol));
    }
}
