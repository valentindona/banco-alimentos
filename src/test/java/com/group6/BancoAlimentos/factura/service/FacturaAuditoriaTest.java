package com.group6.BancoAlimentos.factura.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.institucion.modelo.EstadoPago;
import com.group6.BancoAlimentos.institucion.modelo.Institucion;
import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.annotation.Commit;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.transaction.AfterTransaction;

@DataJpaTest
@ActiveProfiles("test")
class FacturaAuditoriaTest {

    @Autowired
    private InstitucionRepository institucionRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    private Long facturaId;
    private Long institucionId;

    @Test
    @Commit
    void persisteLaProyeccionDeFacturaEnElHistorialDeAuditoria() {
        Institucion institucion = institucionRepository.saveAndFlush(Institucion.builder()
                .nombre("Institución factura " + UUID.randomUUID())
                .domicilio("Calle de auditoría")
                .estadoPago(EstadoPago.AL_DIA)
                .activa(true)
                .build());
        institucionId = institucion.getId();
        Factura factura = Factura.builder()
                .numero("F-" + UUID.randomUUID())
                .institucionId(institucion.getId())
                .fecha(LocalDate.now())
                .estado("PENDIENTE")
                .importe(new BigDecimal("1250.00"))
                .build();
        entityManager.persist(factura);
        entityManager.flush();
        facturaId = factura.getId();
    }

    @AfterTransaction
    void verificaQueLosCambiosDeFacturaConservenRevisiones() {
        if (facturaId == null) {
            return;
        }

        EntityManager em = entityManagerFactory.createEntityManager();
        try {
            var auditor = AuditReaderFactory.get(em);
            var revisiones = auditor.getRevisions(Factura.class, facturaId);
            assertTrue(revisiones.size() >= 1);
            assertEquals(
                    "PENDIENTE",
                    auditor.find(Factura.class, facturaId, revisiones.get(0)).getEstado());
        } finally {
            em.getTransaction().begin();
            Factura factura = em.find(Factura.class, facturaId);
            Institucion institucion = em.find(Institucion.class, institucionId);
            if (factura != null) {
                em.remove(factura);
            }
            if (institucion != null) {
                em.remove(institucion);
            }
            em.getTransaction().commit();
            em.close();
        }
    }
}
