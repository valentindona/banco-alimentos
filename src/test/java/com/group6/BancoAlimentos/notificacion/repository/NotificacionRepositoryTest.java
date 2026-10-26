package com.group6.BancoAlimentos.notificacion.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.group6.BancoAlimentos.notificacion.modelo.Notificacion;
import com.group6.BancoAlimentos.notificacion.modelo.TipoNotificacion;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import com.group6.BancoAlimentos.producto.repository.ProductoRepository;
import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import com.group6.BancoAlimentos.stock.repository.StockRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.envers.Audited;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class NotificacionRepositoryTest {

    @Autowired
    private NotificacionRepository notificacionRepository;

    @Autowired
    private StockRepository stockRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Test
    void persisteEventosIndependientesPorExistenciaYNoAuditaNotificaciones() {
        Stock stock = crearStock();
        Notificacion porVencer = notificacion(stock, TipoNotificacion.POR_VENCER);
        Notificacion vencido = notificacion(stock, TipoNotificacion.VENCIDO);

        Notificacion guardadaPorVencer = notificacionRepository.saveAndFlush(porVencer);
        Notificacion guardadaVencida = notificacionRepository.saveAndFlush(vencido);

        assertTrue(guardadaPorVencer.getId() > 0);
        assertTrue(guardadaVencida.getId() > 0);
        assertNotNull(guardadaPorVencer.getFechaCreacion());
        assertEquals("user_deposito", guardadaPorVencer.getAudiencia());
        assertEquals("user_deposito", guardadaVencida.getAudiencia());
        assertTrue(notificacionRepository.existsByStockIdAndTipo(stock.getId(), TipoNotificacion.POR_VENCER));
        assertTrue(notificacionRepository.existsByStockIdAndTipo(stock.getId(), TipoNotificacion.VENCIDO));
        assertFalse(Notificacion.class.isAnnotationPresent(Audited.class));
    }

    @Test
    void impideDosEventosDelMismoTipoParaLaMismaExistencia() {
        Stock stock = crearStock();
        notificacionRepository.saveAndFlush(notificacion(stock, TipoNotificacion.VENCIDO));

        assertThrows(DataIntegrityViolationException.class, () -> notificacionRepository.saveAndFlush(
                notificacion(stock, TipoNotificacion.VENCIDO)));
    }

    private Stock crearStock() {
        Producto producto = productoRepository.saveAndFlush(Producto.builder()
                .nombre("Producto notificacion")
                .unidadMedida("kg")
                .build());
        return stockRepository.saveAndFlush(Stock.builder()
                .productoId(producto.getId())
                .fechaVencimiento(LocalDate.now().plusDays(2))
                .cantidadDisponible(new BigDecimal("5.000"))
                .estado(EstadoStock.DISPONIBLE)
                .build());
    }

    private Notificacion notificacion(Stock stock, TipoNotificacion tipo) {
        return Notificacion.builder()
                .stockId(stock.getId())
                .tipo(tipo)
                .audiencia("user_deposito")
                .mensaje("Producto Producto notificacion vence pronto; disponible: 5.000 kg.")
                .build();
    }
}
