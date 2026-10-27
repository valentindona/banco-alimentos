package com.group6.BancoAlimentos.itemdonacion.service;

import com.group6.BancoAlimentos.Common.excepcion.ReglaNegocioException;
import com.group6.BancoAlimentos.donacion.modelo.Donacion;
import com.group6.BancoAlimentos.itemdonacion.modelo.ItemDonacion;
import com.group6.BancoAlimentos.itemdonacion.repository.ItemDonacionRepository;
import com.group6.BancoAlimentos.producto.modelo.Producto;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemDonacionService {

    private final ItemDonacionRepository itemDonacionRepository;

    public ItemDonacionService(ItemDonacionRepository itemDonacionRepository) {
        this.itemDonacionRepository = itemDonacionRepository;
    }

    @Transactional
    public ItemDonacion crear(
            Donacion donacion, Producto producto, BigDecimal cantidad, LocalDate fechaVencimiento) {
        return crear(
                donacion,
                producto,
                producto == null ? null : producto.getNombre(),
                producto == null ? null : producto.getTipoAlimento(),
                producto == null ? null : producto.getUnidadMedida(),
                cantidad,
                fechaVencimiento);
    }

    @Transactional
    public ItemDonacion crear(
            Donacion donacion,
            Producto producto,
            String nombreSnapshot,
            String tipoAlimentoSnapshot,
            String unidadMedidaSnapshot,
            BigDecimal cantidad,
            LocalDate fechaVencimiento) {
        if (donacion == null || donacion.getFecha() == null) {
            throw new ReglaNegocioException("La donación y su fecha son obligatorias");
        }
        if (producto == null || !producto.isActivo()) {
            throw new ReglaNegocioException("El producto no existe o está inactivo");
        }
        if (vacio(nombreSnapshot) || vacio(tipoAlimentoSnapshot) || vacio(unidadMedidaSnapshot)) {
            throw new ReglaNegocioException("Nombre, tipo de alimento y unidad de medida son obligatorios");
        }
        validarCantidad(cantidad);
        if (fechaVencimiento == null
                || !fechaVencimiento.isAfter(donacion.getFecha())
                || !fechaVencimiento.isAfter(LocalDate.now())) {
            throw new ReglaNegocioException("El vencimiento debe ser posterior a la recepción y a hoy");
        }

        return itemDonacionRepository.save(ItemDonacion.builder()
                .donacion(donacion)
                .producto(producto)
                .productoNombre(nombreSnapshot.trim())
                .tipoAlimento(tipoAlimentoSnapshot.trim())
                .unidadMedida(unidadMedidaSnapshot.trim())
                .cantidad(cantidad.setScale(3))
                .fechaVencimiento(fechaVencimiento)
                .build());
    }

    private void validarCantidad(BigDecimal cantidad) {
        if (cantidad == null || cantidad.signum() <= 0 || cantidad.scale() > 3) {
            throw new ReglaNegocioException("La cantidad debe ser positiva y tener hasta tres decimales");
        }
    }

    private boolean vacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
