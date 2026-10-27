package com.group6.BancoAlimentos.factura.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.factura.modelo.Factura;
import com.group6.BancoAlimentos.factura.repository.FacturaRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FacturaService {

    private static final String NUMERO_PENDIENTE = "PENDIENTE";
    private static final String ESTADO_EMITIDA = "EMITIDA";

    private final FacturaRepository facturaRepository;

    public FacturaService(FacturaRepository facturaRepository) {
        this.facturaRepository = facturaRepository;
    }

    @Transactional
    public Factura emitir(Long donanteId, Long donacionId, LocalDate fecha) {
        if (donanteId == null || donacionId == null || fecha == null) {
            throw new ReglaNegocioException("Donante, donación y fecha son obligatorios para emitir el comprobante");
        }
        if (facturaRepository.existsByDonacionId(donacionId)) {
            throw new ReglaNegocioException("La donación ya tiene un comprobante asociado");
        }

        Factura factura = facturaRepository.saveAndFlush(Factura.builder()
                .numero(NUMERO_PENDIENTE)
                .donanteId(donanteId)
                .donacionId(donacionId)
                .fecha(fecha)
                .estado(ESTADO_EMITIDA)
                .build());
        factura.setNumero(factura.getId().toString());
        return facturaRepository.saveAndFlush(factura);
    }
}
