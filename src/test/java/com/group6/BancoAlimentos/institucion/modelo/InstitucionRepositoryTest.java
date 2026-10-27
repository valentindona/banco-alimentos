package com.group6.BancoAlimentos.institucion.modelo;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group6.BancoAlimentos.institucion.repository.InstitucionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class InstitucionRepositoryTest {

    @Autowired
    private InstitucionRepository institucionRepository;

    @Test
    void requiereNombreDomicilioTipoYEstadoDePagoPermitido() {
        for (EstadoPago estadoPago : EstadoPago.values()) {
            Institucion institucion = Institucion.builder()
                    .nombre("Institución " + estadoPago)
                    .domicilio("Calle de prueba 123")
                    .estadoPago(estadoPago)
                    .activa(true)
                    .build();
            assertTrue(institucionRepository.saveAndFlush(institucion).getId() > 0);
        }

        Institucion institucionSinTipo = Institucion.builder()
                .nombre("Sin tipo")
                .domicilio("Calle de prueba")
                .estadoPago(EstadoPago.AL_DIA)
                .build();
        assertTrue(institucionRepository.saveAndFlush(institucionSinTipo).getId() > 0);
    }
}
