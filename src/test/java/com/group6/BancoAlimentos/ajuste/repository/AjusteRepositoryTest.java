package com.group6.BancoAlimentos.ajuste.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.ajuste.modelo.Ajuste;
import com.group6.BancoAlimentos.ajuste.modelo.TipoAjuste;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;

@DataJpaTest
@ActiveProfiles("test")
class AjusteRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long ajusteId;

    @Test
    @Commit
    void persisteEventoInmutableYEnversAuditaLaInsercion() {
        Ajuste ajuste = new Ajuste(
                900001L,
                900001L,
                LocalDate.of(2030, 12, 31),
                900001L,
                Instant.parse("2020-01-01T12:00:00Z"),
                TipoAjuste.BAJA,
                new BigDecimal("25.000"),
                new BigDecimal("22.500"),
                "Producto dañado");
        entityManager.persist(ajuste);
        entityManager.flush();
        ajusteId = ajuste.getId();
        assertTrue(java.util.Arrays.stream(Ajuste.class.getMethods())
                .noneMatch(metodo -> metodo.getName().startsWith("set")));
    }

    @AfterTransaction
    void verificaRevisionYEliminaElEventoDePrueba() {
        if (ajusteId == null) {
            return;
        }
        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            assertEquals("Producto dañado", em.find(Ajuste.class, ajusteId).getMotivo());
            assertEquals(1, jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM ajustes_stock_aud WHERE id = ?", Integer.class, ajusteId));
            assertTrue(AuditReaderFactory.get(em).getRevisions(Ajuste.class, ajusteId).size() >= 1);
        } finally {
            em.close();
            jdbcTemplate.update("DELETE FROM ajustes_stock WHERE id = ?", ajusteId);
        }
    }
}
