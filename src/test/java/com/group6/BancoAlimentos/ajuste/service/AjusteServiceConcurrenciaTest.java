package com.group6.BancoAlimentos.ajuste.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.Security.UsuarioAutenticado;
import com.group6.BancoAlimentos.ajuste.dto.AjusteSolicitud;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import com.group6.BancoAlimentos.usuario.modelo.RolUsuario;
import com.group6.BancoAlimentos.usuario.modelo.Usuario;
import com.group6.BancoAlimentos.usuario.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AjusteServiceConcurrenciaTest {

    @Autowired
    private AjusteService ajusteService;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    void serializaBajasConcurrentesSobreElMismoLote() throws Exception {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Producto concurrencia " + UUID.randomUUID())
                .unidadMedida("kg")
                .build());
        Usuario deposito = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("deposito-concurrencia-" + UUID.randomUUID())
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
        Stock stock = stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(LocalDate.now().plusDays(40))
                .cantidadDisponible(new BigDecimal("10.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
        UsuarioAutenticado usuario = new UsuarioAutenticado(
                deposito.getId(), deposito.getNombreUsuario(), List.of("user_deposito"), null);
        CountDownLatch comenzar = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);

        try {
            var primera = executor.submit(() -> registrarBaja(comenzar, stock.getId(), usuario));
            var segunda = executor.submit(() -> registrarBaja(comenzar, stock.getId(), usuario));
            comenzar.countDown();

            boolean primeraExitosa = primera.get(10, TimeUnit.SECONDS);
            boolean segundaExitosa = segunda.get(10, TimeUnit.SECONDS);

            assertNotEquals(primeraExitosa, segundaExitosa);
            assertEquals(new BigDecimal("4.000"),
                    stockRepository.findById(stock.getId()).orElseThrow().getCantidadDisponible());
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ajustes_stock WHERE stock_id = ?", Integer.class, stock.getId()));
        } finally {
            executor.shutdownNow();
            org.junit.jupiter.api.Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    private boolean registrarBaja(CountDownLatch comenzar, Long stockId, UsuarioAutenticado usuario)
            throws InterruptedException {
        comenzar.await();
        try {
            return transactionTemplate.execute(status -> {
                ajusteService.registrar(
                        new AjusteSolicitud(stockId, TipoAjuste.BAJA, new BigDecimal("6.000"), "Baja concurrente"),
                        usuario);
                return true;
            });
        } catch (ReglaNegocioException excepcion) {
            return false;
        }
    }
}
