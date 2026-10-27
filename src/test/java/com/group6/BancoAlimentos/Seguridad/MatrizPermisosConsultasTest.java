package com.group6.BancoAlimentos.Seguridad;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
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
import org.springframework.http.HttpMethod;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;

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
                "/api/remitos", "/api/donantes", "/api/donaciones", "/api/productos",
                "/api/stock", "/api/instituciones", "/api/facturas")) {
            mockMvc.perform(get(ruta).with(autenticar)).andExpect(status().isOk());
        }
    }

    @Test
    void tesoreriaSoloPuedeConsultarInstituciones() throws Exception {
        mockMvc.perform(get("/api/instituciones")
                        .with(authentication(autenticacion("user_tesoreria", null))))
                .andExpect(status().isOk());
        for (String ruta : List.of(
                "/api/remitos", "/api/donantes", "/api/donaciones", "/api/productos",
                "/api/stock", "/api/facturas")) {
            mockMvc.perform(get(ruta).with(authentication(autenticacion("user_tesoreria", null))))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void institucionalNoPuedeAccederANingunEndpointDeRemitos() throws Exception {
        for (String ruta : List.of(
                "/api/donantes", "/api/donaciones", "/api/productos", "/api/stock",
                "/api/facturas")) {
            mockMvc.perform(get(ruta)
                            .with(authentication(autenticacion("user_institucional", 900001L))))
                    .andExpect(status().isForbidden());
        }

        for (String ruta : List.of(
                "/api/remitos", "/api/remitos/1", "/api/remitos/1/detalles",
                "/api/remitos/1/confirmacion")) {
            for (HttpMethod metodo : List.of(
                    HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE)) {
                mockMvc.perform(request(metodo, ruta)
                                .with(authentication(autenticacion("user_institucional", 900001L))))
                        .andExpect(status().isForbidden());
            }
        }
    }

    @Test
    void lasConsultasIndividualesInexistentesDevuelven404() throws Exception {
        var autenticar = authentication(autenticacion("user_deposito", null));
        mockMvc.perform(get("/api/donantes/999999999").with(autenticar))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/donaciones/999999999").with(autenticar))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/donaciones/999999999/items").with(autenticar))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    @Rollback
    void aplicaFiltrosYPaginacionYRestringeRemitosADeposito() throws Exception {
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
                .tipoAlimento("Cereal")
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
        Donante donante = Donante.builder()
                .nombre("US3 Donante de prueba")
                .activo(true)
                .build();
        entityManager.persist(donante);
        entityManager.flush();
        Donacion donacion = Donacion.builder()
                .donante(donante)
                .fecha(fechaFactura)
                .build();
        entityManager.persist(donacion);
        entityManager.flush();
        ItemDonacion itemDonacion = ItemDonacion.builder()
                .donacion(donacion)
                .producto(producto)
                .productoNombre("Nombre de snapshot")
                .tipoAlimento("Cereal")
                .unidadMedida("kg")
                .cantidad(new BigDecimal("2.500"))
                .fechaVencimiento(vencimiento)
                .build();
        entityManager.persist(itemDonacion);
        entityManager.flush();
        Factura comprobante = Factura.builder()
                .numero("US3-D-" + donacion.getId())
                .donanteId(donante.getId())
                .donacionId(donacion.getId())
                .fecha(fechaFactura)
                .estado("EMITIDA")
                .importe(null)
                .build();
        entityManager.persist(comprobante);
        entityManager.flush();
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
                        .param("tipoAlimento", "cereal")
                        .param("unidadMedida", "kg")
                        .param("activo", "true")
                        .param("pagina", "0")
                        .param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(producto.getId()))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamano").value(1));
        mockMvc.perform(get("/api/donantes")
                        .with(autenticarDeposito)
                        .param("nombre", "US3 Donante")
                        .param("activo", "true")
                        .param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(donante.getId()));
        mockMvc.perform(get("/api/donaciones")
                        .with(autenticarDeposito)
                        .param("donanteId", donante.getId().toString())
                        .param("fechaDesde", fechaFactura.toString())
                        .param("fechaHasta", fechaFactura.toString())
                        .param("productoId", producto.getId().toString())
                        .param("vencimientoDesde", vencimiento.toString())
                        .param("vencimientoHasta", vencimiento.toString())
                        .param("tamano", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(donacion.getId()))
                .andExpect(jsonPath("$.contenido[0].items[0].nombreProducto").value("Nombre de snapshot"));
        mockMvc.perform(get("/api/donaciones/" + donacion.getId()).with(autenticarDeposito))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comprobante.donacionId").value(donacion.getId()))
                .andExpect(jsonPath("$.comprobante.detalles[0].nombreProducto").value("Nombre de snapshot"));
        mockMvc.perform(get("/api/donaciones/" + donacion.getId() + "/items").with(autenticarDeposito))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemDonacion.getId()))
                .andExpect(jsonPath("$[0].nombreProducto").value("Nombre de snapshot"));
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
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/facturas")
                        .with(autenticarDeposito)
                        .param("institucionId", institucionPropia.getId().toString())
                        .param("fechaDesde", fechaFactura.toString())
                        .param("fechaHasta", fechaFactura.toString())
                        .param("estado", "PENDIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].id").value(factura.getId()));
        mockMvc.perform(get("/api/facturas")
                        .with(autenticarDeposito)
                        .param("donanteId", donante.getId().toString())
                        .param("productoId", producto.getId().toString())
                        .param("fechaDesde", fechaFactura.toString())
                        .param("fechaHasta", fechaFactura.toString())
                        .param("fechaVencimiento", vencimiento.toString())
                        .param("estado", "EMITIDA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido.length()").value(1))
                .andExpect(jsonPath("$.contenido[0].donacionId").value(donacion.getId()))
                .andExpect(jsonPath("$.contenido[0].detalles[0].nombreProducto").value("Nombre de snapshot"));

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
