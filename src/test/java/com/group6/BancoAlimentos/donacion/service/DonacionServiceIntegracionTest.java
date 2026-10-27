package com.group6.BancoAlimentos.donacion.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.group6.BancoAlimentos.donacion.dto.DonacionSolicitud;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.donacion.repository.DonacionRepository;
import com.group6.BancoAlimentos.donante.dto.DonanteSolicitud;
import com.group6.BancoAlimentos.donante.modelo.Donante;
import com.group6.BancoAlimentos.donante.repository.DonanteRepository;
import com.group6.BancoAlimentos.donante.service.DonanteService;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.dto.ItemDonacionSolicitud;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import com.group6.BancoAlimentos.producto.dto.ProductoSolicitud;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.producto.service.ProductoService;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:donacion-integracion;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
@ActiveProfiles("test")
class DonacionServiceIntegracionTest {

    @Autowired
    private DonacionService donacionService;

    @Autowired
    private DonanteService donanteService;

    @Autowired
    private ProductoService productoService;

    @Autowired
    private DonanteRepository donanteRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private DonacionRepository donacionRepository;

    @Autowired
    private ItemDonacionRepository itemDonacionRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private FacturaRepository facturaRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void confirmaDonacionItemsStockAuditoriasYComprobanteEnUnaTransaccion() {
        LocalDate fecha = LocalDate.now();
        LocalDate vencimiento = fecha.plusDays(40);
        Long donanteId = donanteService.crear(new DonanteSolicitud(
                "Donante integración " + UUID.randomUUID(), null, null)).id();
        String nombreProducto = "Producto integración " + UUID.randomUUID();
        Long productoId = productoService.crear(new ProductoSolicitud(
                nombreProducto, null, "Granos", "kilos")).id();

        var respuesta = donacionService.confirmar(new DonacionSolicitud(
                donanteId,
                fecha,
                java.util.List.of(
                        new ItemDonacionSolicitud(
                                productoId, "Arroz integral", "Granos", "kilos",
                                new BigDecimal("1.250"), vencimiento),
                        new ItemDonacionSolicitud(
                                productoId, "Arroz seleccionado", "Granos", "kilos",
                                new BigDecimal("2.000"), vencimiento))));

        Donacion donacion = donacionRepository.findById(respuesta.id()).orElseThrow();
        var items = itemDonacionRepository.findByDonacionIdOrderByIdAsc(donacion.getId());
        Stock stock = stockRepository.findByProductoIdAndFechaVencimiento(productoId, vencimiento)
                .orElseThrow();
        Factura factura = facturaRepository.findByDonacionId(donacion.getId()).orElseThrow();
        Donante donante = donanteRepository.findById(donanteId).orElseThrow();
        Producto producto = productoRepository.findById(productoId).orElseThrow();

        assertEquals(2, items.size());
        assertEquals("Arroz integral", items.get(0).getProductoNombre());
        assertEquals("Arroz seleccionado", items.get(1).getProductoNombre());
        assertEquals(new BigDecimal("3.250"), stock.getCantidadDisponible());
        assertEquals(EstadoStock.DISPONIBLE, stock.getEstado());
        assertEquals("EMITIDA", factura.getEstado());
        assertEquals(factura.getId().toString(), factura.getNumero());
        assertEquals(donanteId, factura.getDonanteId());
        assertEquals(donacion.getId(), factura.getDonacionId());
        assertNull(factura.getImporte());
        assertEquals(nombreProducto, producto.getNombre());
        assertNotNull(donante.getId());
        assertAuditado(Donacion.class, donacion.getId());
        assertAuditado(ItemDonacion.class, items.get(0).getId());
        assertAuditado(Stock.class, stock.getId());
        assertAuditado(Factura.class, factura.getId());
    }

    private void assertAuditado(Class<?> tipo, Long id) {
        var entityManager = entityManagerFactory.createEntityManager();
        try {
            assertEquals(
                    1,
                    AuditReaderFactory.get(entityManager).getRevisions(tipo, id).size(),
                    () -> tipo.getSimpleName() + " debe tener una revisión de auditoría");
        } finally {
            entityManager.close();
        }
    }
}
