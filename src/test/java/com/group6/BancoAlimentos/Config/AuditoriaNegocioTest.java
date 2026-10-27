package com.group6.BancoAlimentos.Config;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.group6.BancoAlimentos.detalleremito.modelo.DetalleRemito;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import com.group6.BancoAlimentos.notificacion.modelo.Notificacion;
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
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;

@DataJpaTest
@ActiveProfiles("test")
class AuditoriaNegocioTest {

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private RemitoRepository remitoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long productoAuditadoId;
    private Long stockAuditadoId;
    private Long institucionAuditadaId;
    private Long remitoAuditadoId;

    @Test
    @Commit
    void persisteHistorialDeProductoStockInstitucionYRemito() {
        assertFalse(Usuario.class.isAnnotationPresent(Audited.class));
        assertFalse(Notificacion.class.isAnnotationPresent(Audited.class));
        assertTrue(Producto.class.isAnnotationPresent(Audited.class));
        assertTrue(Stock.class.isAnnotationPresent(Audited.class));
        assertTrue(Institucion.class.isAnnotationPresent(Audited.class));
        assertTrue(Remito.class.isAnnotationPresent(Audited.class));
        assertTrue(DetalleRemito.class.isAnnotationPresent(Audited.class));
        assertTrue(Factura.class.isAnnotationPresent(Audited.class));
        Institucion institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución auditada")
                .domicilio("Calle 123")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
        Usuario usuario = usuarioRepository.saveAndFlush(Usuario.builder()
                .nombreUsuario("deposito-auditoria-" + UUID.randomUUID())
                .claveHash("hash-prueba")
                .rol(RolUsuario.USER_DEPOSITO)
                .activo(true)
                .build());
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Arroz auditado")
                .unidadMedida("kg")
                .build());
        Stock stock = stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(LocalDate.now().plusDays(30))
                .cantidadDisponible(new BigDecimal("10.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
        Remito remito = remitoRepository.saveAndFlush(Remito.builder()
                .institucionId(institucion.getId())
                .institucionNombre(institucion.getNombre())
                .institucionDomicilio(institucion.getDomicilio())
                .fechaEntrega(LocalDate.now().plusDays(1))
                .estado(EstadoRemito.BORRADOR)
                .creadoPorUsuarioId(usuario.getId())
                .build());
        entityManager.flush();

        producto.setNombre("Arroz auditado modificado");
        stock.setCantidadDisponible(new BigDecimal("9.000"));
        institucion.setDomicilio("Avenida 456");
        remito.setEstado(EstadoRemito.CONFIRMADO);
        remito.setFechaConfirmacion(java.time.Instant.now());
        productoRepository.saveAndFlush(producto);
        stockRepository.saveAndFlush(stock);
        institucionRepository.saveAndFlush(institucion);
        remitoRepository.saveAndFlush(remito);
        entityManager.flush();

        productoAuditadoId = producto.getId();
        stockAuditadoId = stock.getId();
        institucionAuditadaId = institucion.getId();
        remitoAuditadoId = remito.getId();
    }

    @AfterTransaction
    void verificaRevisionDeCadaEntidadDeNegocio() {
        if (remitoAuditadoId == null) {
            return;
        }
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            var auditor = AuditReaderFactory.get(em);
            assertTrue(auditor.getRevisions(Producto.class, productoAuditadoId).size() >= 1);
            assertTrue(auditor.getRevisions(Stock.class, stockAuditadoId).size() >= 1);
            assertTrue(auditor.getRevisions(Institucion.class, institucionAuditadaId).size() >= 1);
            assertTrue(auditor.getRevisions(Remito.class, remitoAuditadoId).size() >= 1);
        } finally {
            em.close();
        }
    }
}
