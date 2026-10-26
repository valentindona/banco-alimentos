package com.group6.BancoAlimentos.stock.service;

import com.group6.BancoAlimentos.stock.modelo.EstadoStock;
import com.group6.BancoAlimentos.stock.modelo.Stock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class VencimientoStockService {

    private static final int DIAS_AVISO = 3;

    public void actualizarEstado(Stock stock, LocalDate fechaNegocio) {
        if (stock.getFechaVencimiento() == null) {
            stock.setEstado(EstadoStock.SIN_VENCIMIENTO);
        } else if (!stock.getFechaVencimiento().isAfter(fechaNegocio)) {
            stock.setEstado(EstadoStock.VENCIDO);
        } else {
            stock.setEstado(EstadoStock.DISPONIBLE);
        }
    }

    public boolean esElegibleParaEntrega(Stock stock, LocalDate fechaNegocio) {
        return stock.getFechaVencimiento() != null
                && stock.getFechaVencimiento().isAfter(fechaNegocio)
                && stock.getEstado() == EstadoStock.DISPONIBLE
                && stock.getCantidadDisponible() != null
                && stock.getCantidadDisponible().signum() > 0;
    }

    public boolean estaProximoAVencer(Stock stock, LocalDate fechaNegocio) {
        return esElegibleParaEntrega(stock, fechaNegocio)
                && !stock.getFechaVencimiento().isAfter(fechaNegocio.plusDays(DIAS_AVISO));
    }
}
