package com.group6.BancoAlimentos.notificacion.service;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ProgramadorVencimientos {

    private final ProcesoDiarioVencimientosService procesoDiario;
    private final Clock reloj;

    public ProgramadorVencimientos(
            ProcesoDiarioVencimientosService procesoDiario,
            @Qualifier("relojNegocio") Clock reloj) {
        this.procesoDiario = procesoDiario;
        this.reloj = reloj;
    }

    @Scheduled(
            cron = "${programador.vencimientos.cron:0 0 0 * * *}",
            zone = "${programador.vencimientos.zona:America/Argentina/Buenos_Aires}")
    public void procesarVencimientosDiarios() {
        procesoDiario.procesar(LocalDate.now(reloj));
    }
}
