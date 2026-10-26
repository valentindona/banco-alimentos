package com.group6.BancoAlimentos.detalleremito.modelo;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.detalleremito.repository.DetalleRemitoRepository;
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
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;

@DataJpaTest
@ActiveProfiles("test")
class DetalleRemitoRepositoryTest {

    private Long detalleAuditadoId;

    @Autowired
    private DetalleRemitoRepository detalleRepository;

    @Autowired
    private RemitoRepository remitoRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @BeforeEach
    void limpiarIdAuditoria() {
        detalleAuditadoId = null;
    }

    @Test
    void permiteExistenciasConVencimientosDistintosYRechazaCantidadNoPositiva() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Lentejas")
                .unidadMedida("kg")
                .build());
        Stock primero = crearStock(producto.getId(), LocalDate.now().plusDays(20));
        Stock segundo = crearStock(producto.getId(), LocalDate.now().plusDays(40));
        Remito remito = crearRemito();

        DetalleRemito detalle = detalleRepository.saveAndFlush(
                crearDetalle(remito, primero, new BigDecimal("1.000")));
        assertTrue(detalle.getId() > 0);
        assertTrue(detalleRepository.saveAndFlush(crearDetalle(remito, segundo, new BigDecimal("2.000")))
                .getId() > 0);
        assertThrows(RuntimeException.class, () -> detalleRepository.saveAndFlush(
                crearDetalle(remito, primero, BigDecimal.ZERO)));
        assertThrows(RuntimeException.class, () -> detalleRepository.saveAndFlush(
                crearDetalle(remito, segundo, new BigDecimal("-1.000"))));
    }

    @Test
    @Commit
    void auditaCambiosEnElDetalle() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Avena")
                .unidadMedida("kg")
                .build());
        Stock stock = crearStock(producto.getId(), LocalDate.now().plusDays(20));
        Remito remito = crearRemito();
        DetalleRemito detalle = detalleRepository.saveAndFlush(
                crearDetalle(remito, stock, new BigDecimal("1.000")));
        detalle.setCantidad(new BigDecimal("1.500"));
        detalleRepository.saveAndFlush(detalle);
        entityManager.flush();
        detalleAuditadoId = detalle.getId();
    }

    @AfterTransaction
    void verificaHistorialDelDetalle() {
        if (detalleAuditadoId != null) {
            EntityManager em = entityManagerFactory.createEntityManager();
            try {
                assertTrue(AuditReaderFactory.get(em)
                        .getRevisions(DetalleRemito.class, detalleAuditadoId).size() >= 1);
            } finally {
                em.close();
            }
        }
    }

    @Test
    void impideRepetirLaMismaExistenciaEnUnRemito() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Harina")
                .unidadMedida("kg")
                .build());
        Stock stock = crearStock(producto.getId(), LocalDate.now().plusDays(20));
        Remito remito = crearRemito();
        detalleRepository.saveAndFlush(crearDetalle(remito, stock, new BigDecimal("1.000")));

        assertThrows(DataIntegrityViolationException.class, () -> detalleRepository.saveAndFlush(
                crearDetalle(remito, stock, new BigDecimal("2.000"))));
    }

    private Stock crearStock(Long productoId, LocalDate vencimiento) {
        return stockRepository.saveAndFlush(Stock.builder()
                .productoId(productoId)
                .fechaVencimiento(vencimiento)
                .cantidadDisponible(new BigDecimal("10.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
    }

    private Remito crearRemito() {
        Institucion institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución de prueba")
                .domicilio("Calle de prueba 123")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
        return remitoRepository.saveAndFlush(Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio(institucion.getDomicilio())
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(usuarioRepository.saveAndFlush(Usuario.builder()
                        .nombreUsuario("deposito-detalle-" + UUID.randomUUID())
                        .claveHash("hash-prueba")
                        .rol(RolUsuario.USER_DEPOSITO)
                        .activo(true)
                        .build()).getId())
                .build());
    }

    private DetalleRemito crearDetalle(Remito remito, Stock stock, BigDecimal cantidad) {
        return DetalleRemito.builder()
                .remitoId(remito.getId())
                .stockId(stock.getId())
                .productoId(stock.getProductoId())
                .fechaVencimiento(stock.getFechaVencimiento())
                .productoNombre("Producto")
                .cantidad(cantidad)
                .build();
    }
}
