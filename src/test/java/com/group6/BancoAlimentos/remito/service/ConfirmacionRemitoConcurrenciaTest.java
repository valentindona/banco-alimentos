package com.group6.BancoAlimentos.remito.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.remito.modelo.EstadoRemito;
import com.group6.BancoAlimentos.remito.modelo.Remito;
import com.group6.BancoAlimentos.remito.repository.RemitoRepository;
import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.UUID;

@SpringBootTest(properties = {
        "spring.datasource.url=${MYSQL_TEST_URL}",
        "spring.datasource.username=${MYSQL_TEST_USERNAME:root}",
        "spring.datasource.password=${MYSQL_TEST_PASSWORD:}",
        "spring.flyway.clean-disabled=true",
        "spring.sql.init.mode=never"
})
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "MYSQL_TEST_URL", matches = ".+")
class ConfirmacionRemitoConcurrenciaTest {

    @Autowired
    private ConfirmacionRemitoService servicio;

    @Autowired
    private RemitoRepository remitoRepository;

    @Autowired
    private DetalleRemitoRepository detalleRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void confirmacionesConcurrentesNoGeneranSaldoNegativoNiDescuentoParcial() throws Exception {
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("deposito-concurrencia-" + UUID.randomUUID())
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
        Institucion institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución concurrencia")
                .domicilio("Calle 123")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Stock concurrencia")
                .unidadMedida("kg")
                .build());
        Stock stock = stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(LocalDate.now().plusDays(20))
                .cantidadDisponible(new BigDecimal("10.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
        Remito primero = crearRemito(institucion, usuario);
        Remito segundo = crearRemito(institucion, usuario);
        detalleRepository.saveAndFlush(crearDetalle(primero, stock));
        detalleRepository.saveAndFlush(crearDetalle(segundo, stock));

        var ejecutor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Boolean>> confirmaciones = List.of(
                    () -> confirmar(primero.getId()),
                    () -> confirmar(segundo.getId()));
            var resultados = ejecutor.invokeAll(confirmaciones);
            long confirmados = 0;
            for (var resultado : resultados) {
                if (resultado.get()) {
                    confirmados++;
                }
            }

            assertEquals(1, confirmados);
            assertEquals(new BigDecimal("4.000"),
                    stockRepository.findById(stock.getId()).orElseThrow().getCantidadDisponible());
            assertTrue(remitoRepository.findAllById(List.of(primero.getId(), segundo.getId())).stream()
                    .filter(remito -> remito.getEstado() == EstadoRemito.CONFIRMADO).count() == 1);
        } finally {
            ejecutor.shutdownNow();
            assertTrue(ejecutor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private boolean confirmar(Long remitoId) {
        try {
            servicio.confirmar(remitoId);
            return true;
        } catch (ReglaNegocioException excepcion) {
            return false;
        }
    }

    private Remito crearRemito(Institucion institucion, Usuario usuario) {
        return remitoRepository.saveAndFlush(Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio(institucion.getDomicilio())
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(usuario.getId())
                .build());
    }

    private DetalleRemito crearDetalle(Remito remito, Stock stock) {
        return detalleRepository.saveAndFlush(DetalleRemito.builder()
                .remitoId(remito.getId())
                .stockId(stock.getId())
                .productoId(stock.getProductoId())
                .productoNombre("Stock concurrencia")
                .fechaVencimiento(stock.getFechaVencimiento())
                .cantidad(new BigDecimal("6.000"))
                .build());
    }
}
