package com.group6.BancoAlimentos.ajuste.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.group6.BancoAlimentos.ajuste.modelo.Ajuste;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class AjusteConsultaRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private AjusteRepository ajusteRepository;

    private Instant inicioDia;
    private LocalDate vencimiento;

    @BeforeEach
    void prepararAjustes() {
        inicioDia = Instant.parse("2026-10-27T00:00:00Z");
        vencimiento = LocalDate.of(2030, 12, 31);
        guardar(900001L, 900001L, vencimiento, 900001L, inicioDia.plusSeconds(60), TipoAjuste.BAJA);
        guardar(900001L, 900001L, vencimiento, 900002L, inicioDia.plusSeconds(120), TipoAjuste.CORRECCION);
        guardar(900001L, 900001L, null, 900001L, inicioDia.minusSeconds(1), TipoAjuste.BAJA);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void combinaFiltrosYOrdenaPorFechaEIdDescendentes() {
        Page<Ajuste> resultado = buscar(900001L, vencimiento, inicioDia, inicioDia.plusSeconds(86400),
                900001L, TipoAjuste.BAJA, PageRequest.of(0, 20,
                        Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"))));

        assertEquals(1, resultado.getTotalElements());
        assertEquals(inicioDia.plusSeconds(60), resultado.getContent().getFirst().getFechaHora());
    }

    @Test
    void vencimientoOmitidoNoFiltraYFechaHastaEsExclusiva() {
        Page<Ajuste> resultado = buscar(null, null, inicioDia, inicioDia.plusSeconds(86400),
                null, null, PageRequest.of(0, 20,
                        Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"))));

        assertEquals(2, resultado.getTotalElements());
        assertEquals(inicioDia.plusSeconds(120), resultado.getContent().getFirst().getFechaHora());
    }

    private Page<Ajuste> buscar(
            Long productoId,
            LocalDate fechaVencimiento,
            Instant fechaDesde,
            Instant fechaHasta,
            Long usuarioId,
            TipoAjuste tipo,
            PageRequest pagina) {
        return ajusteRepository.buscarConFiltros(
                productoId, fechaVencimiento, fechaDesde, fechaHasta, usuarioId, tipo, pagina);
    }

    private void guardar(
            Long stockId,
            Long productoId,
            LocalDate fecha,
            Long usuarioId,
            Instant fechaHora,
            TipoAjuste tipo) {
        BigDecimal anterior = new BigDecimal("25.000");
        entityManager.persist(new Ajuste(
                stockId,
                productoId,
                fecha,
                usuarioId,
                fechaHora,
                tipo,
                anterior,
                tipo == TipoAjuste.BAJA ? new BigDecimal("24.000") : anterior,
                "Prueba de historial"));
    }
}
